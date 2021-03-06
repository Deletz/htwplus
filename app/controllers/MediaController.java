package controllers;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import models.Folder;
import models.services.NotificationService;
import org.apache.commons.io.FileUtils;

import models.Group;
import models.Media;
import play.Logger;
import play.Play;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.Security;

import views.html.snippets.videoviewer;

@Security.Authenticated(Secured.class)
public class MediaController extends BaseController {
	
	static Form<Media> mediaForm = Form.form(Media.class);
	final static String tempPrefix = "htwplus_temp";

	/**
	 * Function for a single media view
	 * @param id Id of the media file for finding it
	 * @param keyword The keyword for deciding what should be done with the file. Possible keywords in the moment (view, viewvideo, download)
	 * @return Either the file with content-disposition inline or attachment depending on the given keyword or the rendered videosnippet
	 */
    @Transactional(readOnly=true)	
    public static Result view(Long id, String keyword) {
    	Media media = Media.findById(id);
		Logger.debug("Media: " + media.title);
    	if(Secured.viewMedia(media)) {
			if (media == null) {
				return redirect(controllers.routes.Application.index());
			} else {
				if (keyword.equals("download")) {
					response().setHeader("Content-disposition", "attachment; filename=\"" + media.fileName + "\"");
				}
				//Weiteres Keyword war nötig, da ich mich sonst mit dem Videosnippet ständig im Kreis gedreht hätte bei dieser Herangehensweise
				else if (keyword.equals("viewvideo")) {
					response().setHeader("Content-disposition", "inline; filename=\"" + media.fileName + "\"");
				}
				else if (keyword.equals("view")) {
					//Wenn HTML5 unterstütztes Videoformat, wird Videosnippet gerendert, welches view func mit viewvideo keyword aufruft
					if (media.mimetype.endsWith("mp4") || media.mimetype.endsWith("webm") || media.mimetype.endsWith("ogg")) {
						return ok(videoviewer.render(media));
					}
					else {
						response().setHeader("Content-disposition", "inline; filename=\"" + media.fileName + "\"");
					}

				}

				Logger.debug("return ok(media.file)");
				return ok(media.file);
			}
    	} else {
    		flash("error", "Dazu hast du keine Berechtigung!");
    		return redirect(controllers.routes.Application.index());
     	}
    }


	//Geändert von setPDFVewer zu setPrefix für zukünftige Prefix-Settings
	/**
	 * Function for setting prefix if it is needed
	 * @param id Id of the media file for finding it
	 * @return The found prefix
	 */
	public static String setPrefix(Long id) {
		Media m = Media.findById(id);
		String prefix = "";

		if (m.mimetype.endsWith("pdf")) {
			prefix = "/assets/pdfjs/viewer.html?file=";
		}

		return prefix;
	}

	/**
	 * Function for setting the right Colorbox class so the file can be displayed nicely
	 * @param id Id of the media file for finding it
	 * @return The correct class for Colorbox depending on the mimetype
	 */
	public static String addColorboxClass(Long id) {
		Media m = Media.findById(id);
		String classString = "";
		if (m.mimetype.startsWith("image")) {
			classString = "colorboxImage";
		}
		else if (m.mimetype.endsWith("mp4") || m.mimetype.endsWith("webm") || m.mimetype.endsWith("ogg")) {
			classString = "colorboxInlineVideo";
		}

		return classString;
	}

	/**
	 * Function for setting the right glyphicon via bootstrap
	 * @param id Id of the media file for finding it
	 * @return The string for the correct glyphicon
	 */
	public static String glyph(Long id){
		Media m = Media.findById(id);
		String glyphicon = "glyphicon-file";
		String mime = m.mimetype;
		if(mime.endsWith("pdf")) {
			glyphicon = "glyphicon glyphicon-align-center";
		}
		if(mime.startsWith("text"))
		{
			glyphicon = "glyphicon-align-justify";
		}
		if(mime.startsWith("image"))
		{
			glyphicon = "glyphicon-picture";
		}
		if(mime.startsWith("video"))
		{
			glyphicon = "glyphicon-film";
		}
		if(mime.startsWith("audio"))
		{
			glyphicon = "glyphicon-music";
		}
		return glyphicon;
	}
    
    @Transactional
    public static Result delete(Long id) {
    	Media media = Media.findById(id);
    	
    	Call ret = controllers.routes.Application.index();
    	if(media.belongsToGroup()){
    		Group group = media.group;
    		if(!Secured.deleteMedia(media)){
                flash("error", "Dazu hast du keine Berechtigung!");
				return redirect(controllers.routes.Application.index());
    		}
            ret = routes.FolderController.listFolder(media.group.id, media.inFolder.id);
    	}
    	media.delete();
		flash("success", "Datei \"" + media.title + "\" wurde erfolgreich gelöscht!");
    	return redirect(ret);
    }	
    
    @Transactional(readOnly=true)
    public static Result multiView(String target, Long id) {
		Logger.debug("use multiView");
		Call ret = controllers.routes.Application.index();
		Group group = null;
		String filename = "result.zip";
		if(target.equals(Media.GROUP)) {
			group = Group.findById(id);
			if(!Secured.viewGroup(group)){
				Logger.debug("Secured.viewGroup: false");
				return redirect(controllers.routes.Application.index());
			}
			filename = createFileName(group.title);
			ret = controllers.routes.GroupController.media(id);
		} else {
			return redirect(ret);
		}


    	String[] selection = request().body().asFormUrlEncoded().get("selection");

    	List<Media> mediaList = new ArrayList<Media>();

    	if(selection != null) {
           	for (String s : selection) {
        		Media media = Media.findById(Long.parseLong(s));
        		if(Secured.viewMedia(media)) {
            		mediaList.add(media);

        		} else {
        			flash("error", "Dazu hast du keine Berechtigung!");
        			return redirect(controllers.routes.Application.index());
        		}
           	}
			Logger.debug("mediaList: " + mediaList.size());
    	} else {
    		flash("error", "Bitte wähle mindestens eine Datei aus.");
    		return redirect(ret);
    	}

		for (Media m: mediaList) {
			Logger.debug("Media: " + m.title);
		}
		try {
			Logger.debug("create " + filename + " ...");
			File file = createZIP(mediaList, filename);
			Logger.debug(filename + " created");
					response().setHeader("Content-disposition", "attachment; filename=\"" + filename + "\"");
			Logger.debug("return ok(file)");
	    	return ok(file);
		} catch (IOException e) {
			flash("error", "Etwas ist schiefgegangen. Bitte probiere es noch einmal!");
			return redirect(ret);
		}
    }
    
    private static String createFileName(String prefix) {
    	return prefix + "-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".zip";
    }
    
    
    private static File createZIP(List<Media> media, String fileName) throws IOException {
    	
       	//cleanUpTemp(); // JUST FOR DEVELOPMENT, DO NOT USE IN PRODUCTION
	    String tmpPath = Play.application().configuration().getString("media.tempPath");
    	File file = File.createTempFile(tempPrefix, ".tmp", new File(tmpPath));
    	
    	ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file));
    	zipOut.setLevel(Deflater.NO_COMPRESSION);
        byte[] buffer = new byte[4092];
        int byteCount = 0;
    	for (Media m : media) {
    		zipOut.putNextEntry(new ZipEntry(m.fileName));
    		FileInputStream fis = new FileInputStream(m.file);
            byteCount = 0;
            while ((byteCount = fis.read(buffer)) != -1)
            {
            	zipOut.write(buffer, 0, byteCount);
            }
            fis.close();
            zipOut.closeEntry();
		}
    	
    	zipOut.flush();
    	zipOut.close();
    	return file;
    }

    
    /**
     * Size of temporary media directoy used for ZIP Downloads
     */
    public static long sizeTemp() {
	    String tmpPath = Play.application().configuration().getString("media.tempPath");
	    File dir = new File(tmpPath);
	    return FileUtils.sizeOfDirectory(dir);
    }
    
    /**
     * Cleans the temporary media directoy used for ZIP Downloads
     */
    public static void cleanUpTemp() {
	    Logger.info("Cleaning the Tempory Media Directory");

	    String tmpPath = Play.application().configuration().getString("media.tempPath");
	    File dir = new File(tmpPath);
	    Logger.info("Directory: " + dir.toString());
	    File[] files = dir.listFiles();
	    Logger.info("Absolut Path: " + dir.getAbsolutePath());
	    
	    if(files != null) {
		    // Just delete files older than 1 hour
		    long hours = 1;
			long eligibleForDeletion = System.currentTimeMillis() - (hours * 60 * 60 * 1000L);
		    
		    Logger.info("Found " + files.length + " Files");
		    if(files != null){	    
			    for (File file : files) {
			    	Logger.info("Working on " + file.getName());
					if(file.getName().startsWith(tempPrefix) && file.lastModified() < eligibleForDeletion) {
						Logger.info("Deleting: " + file.getName());
						file.delete();
					}
				}
		    }
	    } else {
	    	Logger.info("files is null");
	    }
	    
    }

    /**
     * New file is uploaded.
     *
     * @param target Target of the file (e.g. "group")
     * @param id ID of the target (e.g. group ID)
     * @return Result
     */
	@Transactional
    public static Result upload(Long id, Long folderID) {
	    final int maxTotalSize = Play.application().configuration().getInt("media.maxSize.total");
	    final int maxFileSize = Play.application().configuration().getInt("media.maxSize.file");

		Call ret = controllers.routes.Application.index();
		Group group = Group.findById(id);
		Folder folder = Folder.findById(folderID);

        // Where to put the media
		if (!Secured.uploadMedia(group)) {
			return redirect(controllers.routes.Application.index());
		}
		ret = controllers.routes.FolderController.listFolder(group.id, folder.id);
		
		// Is it to big in total?
		String[] contentLength = request().headers().get("Content-Length");
		if (contentLength != null) {
			int size = Integer.parseInt(contentLength[0]);
			if(Media.byteAsMB(size) > maxTotalSize) {
				return ok("Du darfst auf einmal nur " + maxTotalSize + " MB hochladen.");
				/*flash("error", "Du darfst auf einmal nur " + maxTotalSize + " MB hochladen.");
				return redirect(ret);*/
			}
		} else {
			return ok("Etwas ist schiefgegangen. Bitte probiere es noch einmal!");
//			flash("error", "Etwas ist schiefgegangen. Bitte probiere es noch einmal!");
//		    return redirect(ret);
		}
		
		// Get the data
		MultipartFormData body = request().body().asMultipartFormData();
		List<Http.MultipartFormData.FilePart> uploads = body.getFiles();

		List<Media> mediaList = new ArrayList<Media>();
		
		if (!uploads.isEmpty()) {

			
			// Create the Media models and perform some checks
			for (FilePart upload : uploads) {
				
				Media med = new Media();
				med.title = upload.getFilename();
				med.fileName = upload.getFilename();
				// added this code snippet for fixing the upload bug from firefox where pdf file has the mime typ "application/binary or other
				if (upload.getContentType().endsWith("binary") && request().headers().get("User-Agent")[0].contains("Mozilla") &&  med.fileName.endsWith(".pdf")) {
					med.mimetype = "application/pdf";
					Logger.info ("PDF upload with Mozilla, set Mimetype manuell");
				}
				else {
					med.mimetype = upload.getContentType();
				}
				med.file = upload.getFile();				
				med.owner = Component.currentAccount();
				med.inFolder = folder;
				
				if (Media.byteAsMB(med.file.length()) > maxFileSize) {
					return ok("Die Datei " + med.title + " ist größer als " + maxFileSize + " MB!");
//					flash("error", "Die Datei " + med.title + " ist größer als " + maxFileSize + " MB!");
//					return redirect(ret);
				}

                med.temporarySender = Component.currentAccount();
                med.group = group;
                if (FolderController.mediaExistInFolder(med,folder)) {
                    return ok("Eine Datei mit dem Namen " + med.title + " existiert bereits");
//                    flash("error", "Eine Datei mit dem Namen " + med.title + " existiert bereits");
//                    return redirect(ret);
                }

				mediaList.add(med);
			}
			
			for (Media m : mediaList) {
				try {
					m.create();

                    // create group notification, if a group exists
                    if (m.group != null) {
                        NotificationService.getInstance().createNotification(m, Media.MEDIA_NEW_MEDIA);
                    }
				} catch (Exception e) {
					return internalServerError(e.getMessage());
				}
			}
            return ok("Datei(en) erfolgreich hinzugefügt.");
//			flash("success", "Datei(en) erfolgreich hinzugefügt.");
//		    return redirect(ret);
		} else {
			flash("error", "Etwas ist schiefgegangen. Bitte probiere es noch einmal!");
		    return redirect(ret);  
		}
    }
	
	public static String bytesToString(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
}