package it.nextworks.nfvmano.catalogue.auth;

import io.swagger.annotations.ApiParam;
import it.nextworks.nfvmano.catalogue.auth.Resources.ProjectResource;
import it.nextworks.nfvmano.catalogue.auth.Resources.UserResource;
import it.nextworks.nfvmano.catalogue.common.Utilities;
import it.nextworks.nfvmano.catalogue.nbi.sol005.nsdmanagement.elements.ProblemDetails;
import it.nextworks.nfvmano.catalogue.repos.ProjectRepository;
import it.nextworks.nfvmano.catalogue.repos.UserRepository;
import it.nextworks.nfvmano.libs.common.exceptions.AlreadyExistingEntityException;
import it.nextworks.nfvmano.libs.common.exceptions.FailedOperationException;
import it.nextworks.nfvmano.libs.common.exceptions.MalformattedElementException;
import it.nextworks.nfvmano.libs.common.exceptions.NotPermittedOperationException;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/catalogue/userManagement")
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeycloakService keycloakService;

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    public UserController() {
    }


    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public ResponseEntity<?> getUsers(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        log.debug("Received request for getting Users");

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        List<UserRepresentation> users;
        List<UserResource> userResources;
        try {
            users = keycloakService.getUsers();
            for (UserRepresentation userRepresentation : users) {
                Optional<UserResource> userResourceOptional = userRepository.findByUserName(userRepresentation.getUsername());
                if (!userResourceOptional.isPresent()) {
                    UserResource userResource = new UserResource(userRepresentation.getUsername(), userRepresentation.getFirstName(), userRepresentation.getLastName());
                    userResource.setExternalId(userRepresentation.getId());
                    userRepository.saveAndFlush(userResource);
                }
            }
            userResources = userRepository.findAll();
        } catch (FailedOperationException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (NotPermittedOperationException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<List<UserResource>>(userResources, HttpStatus.OK);
    }

    @RequestMapping(value = "/users/{userName}", method = RequestMethod.GET)
    public ResponseEntity<?> getUser(@ApiParam(value = "", required = true)
                                     @PathVariable("userName") String userName,
                                     @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        log.debug("Received request for getting User " + userName);

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        UserResource userResource;
        Optional<UserResource>  optional = userRepository.findByUserName(userName);
        if (optional.isPresent()) {
            userResource = optional.get();
            return new ResponseEntity<UserResource>(userResource, HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("User with userName " + userName + " not present in DB", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/users", method = RequestMethod.POST)
    public ResponseEntity<?> createUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                        @RequestBody UserResource user) {

        log.debug("Received request for new User creation");

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        if ((user == null) || (user.getUserName() == null)) {
            log.error("Malformatted User - Not acceptable");
            return new ResponseEntity<String>("User or userName null", HttpStatus.BAD_REQUEST);
        }

        ProjectResource projectResource;
        Optional<ProjectResource> optional = projectRepository.findByProjectId(user.getDefaultProject());
        Optional<UserResource> userResourceOptional = userRepository.findByUserName(user.getUserName());
        if (!optional.isPresent()) {
            return new ResponseEntity<String>("Default project not present in DB", HttpStatus.BAD_REQUEST);
        } else if (userResourceOptional.isPresent()) {
            return new ResponseEntity<String>("User with userName " + user.getUserName() + " already exists", HttpStatus.CONFLICT);
        } else {
            UserRepresentation userRepresentation =
                    keycloakService.buildUserRepresentation(user.getUserName(), user.getFirstName(), user.getLastName());
            try {
                UserRepresentation createdUser = keycloakService.createUser(userRepresentation);
                keycloakService.addUserToGroup(createdUser.getId());

                user.addProject(user.getDefaultProject());
                user.setExternalId(createdUser.getId());
                userRepository.saveAndFlush(user);
                projectResource = optional.get();
                projectResource.addUser(user.getUserName());
                projectRepository.saveAndFlush(projectResource);
            } catch (FailedOperationException e) {
                return new ResponseEntity<String>(e.getMessage(), HttpStatus.UNAUTHORIZED);
            } catch (AlreadyExistingEntityException e) {
                return new ResponseEntity<String>(e.getMessage(), HttpStatus.CONFLICT);
            } catch (MalformattedElementException e) {
                return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
            } catch (NotPermittedOperationException e) {
                return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
            }
        }

        return new ResponseEntity<UserResource>(user, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/projects/{projectId}/users/{userName}", method = RequestMethod.PUT)
    public ResponseEntity<?> addUserToProject(@ApiParam(value = "", required = true)
                                              @PathVariable("projectId") String projectId,
                                              @ApiParam(value = "", required = true)
                                              @PathVariable("userName") String userName,
                                              @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        log.debug("Received request for adding User " + userName + " to project " + projectId);

        if(keycloakEnabled && authorization == null){
            return new ResponseEntity<ProblemDetails>(
                    Utilities.buildProblemDetails(HttpStatus.UNAUTHORIZED.value(),
                            "Missing request header 'Authorization'"),
                    HttpStatus.UNAUTHORIZED);
        }

        ProjectResource projectResource;
        Optional<ProjectResource> optional = projectRepository.findByProjectId(projectId);
        if (optional.isPresent()) {
            projectResource = optional.get();
            projectResource.addUser(userName);
            projectRepository.saveAndFlush(projectResource);
            log.debug("User " + userName + " successfully added to project " + projectId);
            Optional<UserResource> userResourceOptional = userRepository.findByUserName(userName);
            if (userResourceOptional.isPresent()) {
                UserResource userResource = userResourceOptional.get();
                if (userResource.getProjects().isEmpty()) {
                    userResource.setDefaultProject(projectId);
                }
                userResource.addProject(projectId);
                userRepository.saveAndFlush(userResource);
            } else {
                return new ResponseEntity<String>("User with userName " + userName + " not present in DB", HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<ProjectResource>(projectResource, HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("Project with projectId " + projectId + " not present in DB", HttpStatus.BAD_REQUEST);
        }
    }
}
