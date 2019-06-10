package it.nextworks.nfvmano.catalogue.auth;

import io.swagger.annotations.ApiParam;
import it.nextworks.nfvmano.catalogue.auth.Resources.ProjectResource;
import it.nextworks.nfvmano.catalogue.repos.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping("/catalogue/projectManagement")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    ProjectRepository projectRepository;

    public ProjectController() {
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
            createdProjectResource = new ProjectResource(project.getProjectId(), project.getProjectDescription());
            projectRepository.saveAndFlush(project);
        }

        return new ResponseEntity<String>(createdProjectResource.getProjectId().toString(), HttpStatus.CREATED);
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

        Optional<ProjectResource> optional = projectRepository.findByProjectId(UUID.fromString(projectId));
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
        Optional<ProjectResource> optional = projectRepository.findByProjectId(UUID.fromString(projectId));
        if (optional.isPresent()) {
            projectResource = optional.get();
            if (projectResource.isDeletable()) {
                projectRepository.delete(projectResource);
                return new ResponseEntity<String>(projectId, HttpStatus.OK);
            } else {
                return new ResponseEntity<String>("Project canot be deleted because in use", HttpStatus.CONFLICT);
            }
        } else {
            return new ResponseEntity<String>("Project with projectId " + projectId + " not present in DB", HttpStatus.BAD_REQUEST);
        }
    }
}
