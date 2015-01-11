package controllers;

import com.fasterxml.jackson.databind.deser.Deserializers;
import models.Folder;
import models.Group;
import models.Media;
import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by meichris on 06.12.14.
 */
@Security.Authenticated(Secured.class)
public class FolderController extends BaseController {


    public static Long createFolder(String argName, Long argParentId) {
        Folder f = new Folder();
        if (allowToCreate(argName,argParentId)) {
            f.name = argName;
            f.group = Folder.findById(argParentId).group;
            if (argParentId != null) {
                f.parent = Folder.findById(argParentId);
                f.depth = f.parent.depth + 1;
            }
            f.create();
        }
        return f.id;
    }

    public static List<Media> getAllMediaInFolder(Long argId){
        List<Media> medias = new ArrayList<Media>();
        try {
            Folder f = Folder.findById(argId);
            medias = f.files;
        } catch (NullPointerException e) {
            //Blabla
        }
        return medias;
    }

    private static boolean allowToCreate(String argName, Long argParentId) {
        boolean allow = true;
        List<Folder> folders = Folder.findById(argParentId).childs;
        if (folders != null)
            for (Folder f: folders)
                if (f.name.equals(argName))
                    allow =false;
        return allow;
    }

    public static Long getGroupFolder(String argGroupName) {
        List<Folder> f = JPA.em().createNamedQuery(Folder.QUERY_FIND_ALL_GROUPFOLDER).getResultList();
        return f.get(0).id;
    }
    
    ///// TO CHANGE
    

	@Transactional
	public static Result listFolder(Long folderID) {
		Folder f = Folder.findById(folderID);
		Folder folder = f;
		List<Folder> path = new ArrayList<Folder>();
		List<Folder> pathTemp = new ArrayList<Folder>();
		if (f == null)
			return redirectFolderError("Ordner nicht vorhanden, wählen Sie einen anderen!");
		while (f.depth > 1) {
			pathTemp.add(f);
			f = f.parent;
		}
		for (int i = pathTemp.size()-1; i >= 0; i--)
			path.add(pathTemp.get(i));
		return ok(folder.toAlternateString());
	//	return ok(folder.render(path,folder));
	}

    @Transactional
    public static Result listFolder2(Long groupID,Long folderID) {
        Folder folder = Folder.findById(folderID);
        if(Secured.viewFolder(folder)) {
            Folder folderTemp = folder;
            List<Folder> path = new ArrayList<Folder>();
            List<Folder> pathTemp = new ArrayList<Folder>();
            Folder groupFolder = (Folder) JPA.em().createNamedQuery(Folder.QUERY_FIND_ROOT_OF_GROUP).setParameter(Folder.PARAM_GROUP_ID, groupID).getSingleResult();
            if (folder == null || groupID != folder.group.id) {
                Logger.debug("No Folder Found. Back to groupFolder");
                return redirectGroupFolder(groupID, groupFolder.id);
            }
            while (folderTemp.depth > 0) {
                pathTemp.add(folderTemp);
                folderTemp = folderTemp.parent;
            }
            for (int i = pathTemp.size()-1; i >= 0; i--)
                path.add(pathTemp.get(i));
            //	return ok(folder.render(path,folder));
            return ok(folderContentToString(path,folder));
        } else {
            flash("error", "Dazu hast du keine Berechtigung!");
            return redirect(controllers.routes.Application.index());
        }

    }
    

    public static Result redirectFolder(Long folderID) {
		return redirect("/folder/" + folderID);
	}

    public static Result redirectGroupFolder(Long groupID, Long folderID) {
        return redirect("/group/" + groupID + "/media/" + folderID);
    }

	public static Result redirectFolderError(String error) {
		return ok("NOT OKAY");
		//return ok(test.render(error));
	}
	
	public static Result createMedia(Long id) {
		return ok("OKAY");
	}
	

	@Transactional
	public static Result createFolder(Long folderID) {

//		String uri = request().uri();
//		int index = uri.lastIndexOf("/");
//		Long folder = Long.valueOf(uri.substring(index));

        FolderController.createFolder("Ordner xyz", folderID);
        return redirectFolder(folderID);
    }

    private static String folderContentToString(List<Folder> path, Folder folder) {
        String ret = "";
        for (Folder f:path)
            ret = ret + f.name + "/";
        ret += "\n\n";
        List<Folder> childs = folder.childs;
        List<Media> files = folder.files;
        int file = 1;
        for (Folder f: childs)
            ret = ret + f.toAlternateString() + "\n";
        ret += "\n\n";
            ret = ret + "Files: " + files.size() + "\n";
        return ret;
    }


}
