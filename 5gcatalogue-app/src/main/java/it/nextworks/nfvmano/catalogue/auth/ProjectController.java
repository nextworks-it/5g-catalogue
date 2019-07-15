package it.nextworks.nfvmano.catalogue.auth;

import io.swagger.annotations.ApiParam;
import it.nextworks.nfvmano.catalogue.auth.Resources.ProjectResource;
import it.nextworks.nfvmano.catalogue.auth.Resources.UserResource;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/catalogue/projectManagement")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    KeycloakService keycloakService;

    public ProjectController() {
    }

    @PostConstruct
    public void init() {
        /*try {
            List<UserRepresentation> userRepresentations = keycloakService.getUsers();
        } catch (NotPermittedOperationException e) {
            e.printStackTrace();
        } catch (FailedOperationException e) {
            e.printStackTrace();
        }*/

        ProjectResource project = new ProjectResource("Admins", "Admins project");
        project.addUser("admin");
        Optional<ProjectResource> optional = projectRepository.findByProjectId(project.getProjectId());
        if (!optional.isPresent()) {
            projectRepository.saveAndFlush(project);
            log.debug("Project " + project.getProjectId() + " successfully created");
        }

        UserResource userResource = new UserResource("admin", "Admin", "Admin", "Admins");
        userResource.addProject("Admins");
        Optional<UserResource> optionalUserResource = userRepository.findByUserName(userResource.getUserName());
        if (!optionalUserResource.isPresent()) {
            userRepository.saveAndFlush(userResource);
            log.debug("User " + userResource.getUserName() + " successfully created");
        }
    }

    @RequestMapping(value = "/projects", method = RequestMethod.POST)
    public ResponseEntity<?> createProject(@ApiParam(value = "", required = true)
                                           @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                           @RequestBody ProjectResource project) {

        log.debug("Received request for new Project creation");
        if ((project == null) || (project.getProjectId() == null)) {
            log.error("Malformatted Project - Not acceptable");
            return new ResponseEntity<String>("Project or Project ID null", HttpStatus.BAD_REQUEST);
        }

        ProjectResource createdProjectResource;
        Optional<ProjectResource> optional = projectRepository.findByProjectId(project.getProjectId());
        if (optional.isPresent()) {
            return new ResponseEntity<String>("Project already present in DB", HttpStatus.CONFLICT);
        } else {
            project.addUser("admin");
            createdProjectResource = projectRepository.saveAndFlush(project);
            log.debug("Project " + project.getProjectId() + " successfully created");

            log.debug("Going to update user admin with new created project " + project.getProjectId());
            Optional<UserResource> optionalUserResource = userRepository.findByUserName("admin");
            if (optionalUserResource.isPresent()) {
                UserResource userResource = optionalUserResource.get();
                userResource.addProject(createdProjectResource.getProjectId());
                userRepository.saveAndFlush(userResource);
                log.debug("User admin successfully updated with new project " + createdProjectResource.getProjectId());
            } else {
                log.warn("Unable to update user admin with new created project " + createdProjectResource.getProjectId());
            }

        }

        return new ResponseEntity<ProjectResource>(createdProjectResource, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/projects", method = RequestMethod.GET)
    public ResponseEntity<?> getProjects(@ApiParam(value = "", required = true)
                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        log.debug("Received request for getting Projects");

        List<ProjectResource> projectResources = projectRepository.findAll();

        return new ResponseEntity<List<ProjectResource>>(projectResources, HttpStatus.OK);
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.GET)
    public ResponseEntity<?> getProject(@ApiParam(value = "", required = true)
                                        @PathVariable("projectId") String projectId,
                                        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        log.debug("Received request for getting Project with Project ID " + projectId);

        Optional<ProjectResource> optional = projectRepository.findByProjectId(projectId);
        if (optional.isPresent()) {
            return new ResponseEntity<ProjectResource>(optional.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("Project with projectId " + projectId + " not present in DB", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/projects/{projectId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteProject(@ApiParam(value = "", required = true)
                                           @PathVariable("projectId") String projectId,
                                           @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        log.debug("Received request for deleting Project with Project ID " + projectId);

        ProjectResource projectResource;
        Optional<ProjectResource> optional = projectRepository.findByProjectId(projectId);
        if (optional.isPresent()) {
            projectResource = optional.get();
            if (projectResource.isDeletable()) {
                projectRepository.delete(projectResource);
                log.debug("Project " + projectId + " successfully deleted");
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<String>("Project canot be deleted because in use", HttpStatus.CONFLICT);
            }
        } else {
            return new ResponseEntity<String>("Project with projectId " + projectId + " not present in DB", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public ResponseEntity<?> getUsers(@ApiParam(value = "", required = true)
                                      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        log.debug("Received request for getting Users");

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

        UserResource userResource;
        Optional<UserResource> optional = userRepository.findByUserName(userName);
        if (optional.isPresent()) {
            userResource = optional.get();
            return new ResponseEntity<UserResource>(userResource, HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("User with userName " + userName + " not present in DB", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/users", method = RequestMethod.POST)
    public ResponseEntity<?> createUser(@ApiParam(value = "", required = true)
                                        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                        @RequestBody UserResource user) {

        log.debug("Received request for new User creation");
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
                                              @PathVariable("userName") String userName,
                                              @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        log.debug("Received request for adding User " + userName + " to project " + projectId);

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
