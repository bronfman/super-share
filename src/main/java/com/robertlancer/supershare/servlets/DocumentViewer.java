package com.robertlancer.supershare.servlets;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.robertlancer.supershare.util.Mime;
import com.robertlancer.supershare.util.ServiceFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class DocumentViewer extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    String pathInfo = req.getPathInfo();

    if (pathInfo == null || pathInfo.length() <= 1) {
      sendError(404, resp);
      return;
    }

    String fileTitle = pathInfo.substring(1).replace("-", " ");

    String folderId = System.getProperty("folder");
    String email = System.getProperty("email");

    Drive drive = ServiceFactory.getDriveService(email);
    List<File> items = getFiles(folderId, email);

    File fileToOutput = null;

    for (File file : items) {
      if (file.getTitle().equalsIgnoreCase(fileTitle))
        fileToOutput = file;
    }

    if (fileToOutput == null) {
      sendError(404, resp);
      return;
    } else {

      boolean anyoneHasAccess = false;
      for (Permission permission : fileToOutput.getPermissions()) {
        if (permission.getType().equalsIgnoreCase("anyone")) {
          anyoneHasAccess = true;
          break;
        }
      }

      if (!anyoneHasAccess) {
        Permission permission = new Permission();
        permission.setType("anyone");
        permission.setRole("reader");
        permission.setWithLink(true);

        Permission inserted = ServiceFactory.getDriveService(email).permissions().insert(fileToOutput.getId(), permission).execute();
        System.err.println("Permission upgraded to anyone with the link " + fileToOutput.getTitle());
      }

      resp.getWriter().write(outputFile(fileToOutput));
    }
  }

  public static String outputFile(File fileToOutput) {

    String url = outputFileAsIFrameGetURL(fileToOutput);

    String iframe = "<iframe src='" + url + "' frameborder=0 style='width:100%;height:100%;' /></iframe>";

    StringBuilder output = new StringBuilder();
    output.append("<html>");
    output.append("<head><title>" + fileToOutput.getTitle() + "</title>");
    output.append("<link rel=\"icon\" type=\"image/png\"\n" +
      " href=\"" + fileToOutput.getIconLink() + "\" />");
    output.append("</head>");
    output.append("<body>");
    output.append("<style>\n");
    output.append("html, body { overflow:hidden; height:100%; padding:0px; margin:0px; }");
    output.append("\n</style>");
    output.append(iframe);
    output.append("</body></html>");

    return output.toString();
  }

  public static String outputFileAsIFrameGetURL(File fileToOutput) {

    String domain = fileToOutput.getOwners().get(0).getEmailAddress().split("@")[1];
    String id = fileToOutput.getId();

    switch (fileToOutput.getMimeType()) {
      case Mime.DOCUMENT:
        return "https://docs.google.com/a/" + domain + "/document/d/" + id + "/preview";
      case Mime.SPREADSHEET:
        return "https://docs.google.com/a/" + domain + "/spreadsheet/ccc?key=" + id + "&output=html&widget=true&chrome=false";
      case Mime.PRESENTATION:
        return "https://docs.google.com/a/" + domain + "/presentation/d/" + id + "/preview";
      case Mime.DRAWING:
        return "https://docs.google.com/a/" + domain + "/drawings/d/" + id + "/preview";
      default:
        return "https://docs.google.com/a/" + domain + "/file/d/" + id + "/preview";
    }


  }

  public static List<File> getFiles(String folderId, String email) {
    Drive drive = ServiceFactory.getDriveService(email);
    List<File> items = null;
    try {
      Drive.Files.List request = drive.files().list().setQ("'" + folderId + "' in parents and trashed = false").setFields("items(owners,id,downloadUrl,iconLink,mimeType,permissions,title)");
      return request.execute().getItems();
    } catch (Exception ex) {
      return null;
    }
  }

  public void sendError(int code, HttpServletResponse resp) {
    try {
      resp.setStatus(code);
      if (code == 404)
        resp.getWriter().write("Sorry could not find the document you were looking for.");
      else
        resp.getWriter().write("An internal error occurred please try again soon.");

    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doGet(req, resp);
  }
}