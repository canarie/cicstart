package ca.ualberta.physics.cssdp.vfs.resource;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.wordnik.swagger.annotations.Api;

@Path("/filesystem.json")
@Api(value = "/filesystem", description = "Operations about the files on a users VFS instance")
@Produces({ MediaType.APPLICATION_JSON })
public class FileSystemResourceJSON extends FileSystemResource {

}