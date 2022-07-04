import java.nio.*;
import java.nio.file.*;
import java.net.*;
import java.io.*;
import java.util.logging.Logger;

public class RequestProcessor implements Runnable {
	private static final Logger logger = Logger.getLogger(RequestProcessor.class.getCanonicalName());
	private File dir;
	private Socket socket;

	public RequestProcessor(File dir, Socket socket) {
		this.dir = dir;
		this.socket = socket;
	}


	public void run(){
		try {
			Reader in = new InputStreamReader(new BufferedInputStream(socket.getInputStream()));
			StringBuilder requestLine = new StringBuilder();
			while(true) {
				int c = in.read();
				if(c=='\r' || c=='\n') break;
				requestLine.append((char) c);
			}

			String get = requestLine.toString();
			String[] tokens = get.split("\\s+");
			logger.info(socket.getRemoteSocketAddress()+"<----> "+get);

			String version = "0";
			if(tokens.length>2){
				version = tokens[2].substring(tokens[2].lastIndexOf(".")+1);
			}
			if(tokens[0].equals("GET")){
				doGET(new File(dir,URLDecoder.decode(tokens[1])), version);
			}
			/*if(tokens[0].equals("POST")){
				doPUT(new File(tokens[1]));
			}*/
		} catch (IOException e) {

		} finally {
			try{
				socket.close();
			} catch (IOException e) {

			}
		}
	}

	private void doPUT(File file){
		InputStream inputFile;
		OutputStream outputFile = null;
		try {
			inputFile = socket.getInputStream();
			outputFile = new FileOutputStream(dir.toPath().toString()+"/Uploads"+file.toPath().toString());
			int c;
			while((c = inputFile.read())!=-1){
				outputFile.write(c);
			}
		} catch (IOException ex){

		} finally {
			try{
				outputFile.close();
			} catch (IOException ex) {

			}
		}
	}

	private void doGET(File file, String version) {

		try {
			OutputStream raw = new BufferedOutputStream(socket.getOutputStream());
			Writer out = new OutputStreamWriter(raw);

			if(file.isDirectory()) {

				if(file.canRead() && file.getCanonicalPath().startsWith(dir.getPath())) {
					DirectoryStream<Path> dirstrm = Files.newDirectoryStream(file.toPath());
					String data  = generateHtml(dirstrm);

					sendHeader(out, "HTTP/1."+version+" 200 OK", "text/html", data.getBytes().length);

					out.write(data);
					out.flush();
				} else {
					//permission
				}
			} else {
				String contentType = URLConnection.getFileNameMap().getContentTypeFor(file.toPath().toString());
				logger.info(socket.getRemoteSocketAddress()+"<----> "+file.toPath().toString()+" Content-type="+contentType);

				if(file.canRead() && file.getCanonicalPath().startsWith(dir.getPath())) {
					InputStream f = new FileInputStream(file);
					byte[] bytes = f.readAllBytes();

					sendHeader(out, "HTTP/1."+version+" 200 OK", contentType, bytes.length);

					raw.write(bytes);
					raw.flush();
				} else {
					String err = new StringBuffer().append("<!DOCTYPE html>\r\n<html>\r\n<head>\r\n<title>File Not Found</title>\r\n</head>\r\n<body>\r\n")
					.append("<center><h1>404 : File Not Found</h1></center>\r\n")
					.append("</body>\r\n</html>").toString();

					sendHeader(out, "HTTP/1."+version+" 404 File not found", null, err.getBytes().length);
					out.write(err);
					out.flush();
				}
			}
		} catch (IOException ex) {
			logger.log(java.util.logging.Level.WARNING, "Error talking to " + socket.getRemoteSocketAddress(), ex);
		} finally {
			try {
				socket.close();
			} catch (IOException ex) {

			}
		}
	}

	private String generateHtml(DirectoryStream<Path> dirStream){
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\r\n<html>\r\n<head>\r\n<title>Dir Serve</title>\r\n</head>\r\n<body>\r\n");
		html.append("<h1>Index of Files</h1>\r\n<ul>\r\n");

		for(Path path : dirStream) {
			String holder = path.toString();
			int index = holder.lastIndexOf("/")+1;
			html.append("<li><a href=\""+holder.substring(dir.getPath().length(),holder.length())+"\" download>"+holder.substring(index,holder.length())+"</a></li>\r\n");
		}

		html.append("</ul>\r\n</body>\r\n</html>");

		return html.toString();
	}

	private void sendHeader(Writer out, String responseCode, String contentType, int length) throws IOException {
		out.write(responseCode + "\r\n");
		java.util.Date now = new java.util.Date();
		out.write("Date: " + now + "\r\n");
		out.write("Server: JHTTP 2.0\r\n");
		out.write("Content-length: " + length + "\r\n");
		out.write("Content-type: " + contentType + "\r\n\r\n");
		out.flush();
	}
}