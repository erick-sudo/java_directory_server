import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;


public class DirectoryServer {
	private static final Logger logger = Logger.getLogger(DirectoryServer.class.getCanonicalName());
	private static final int NUM_THREADS = 50;

	private final File directory;
	private final int port;

	public DirectoryServer(File directory, int port) throws IOException {
		if(!directory.isDirectory()){
			throw new IOException(directory+ " does not exist as a directory");
		}

		this.directory = directory;
		this.port = port;
	}

	public void start() throws IOException {
		ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);

		try(ServerSocket server = new ServerSocket(port)) {
			logger.info("Accepting connection on port "+server.getLocalPort());
			logger.info("ROOT DIRECTORY: "+directory);

			while(true){
				try {
					Socket request = server.accept();
					Runnable r = new RequestProcessor(directory, request);
					pool.submit(r);
				} catch (IOException e){
					logger.log(Level.WARNING, "Error accepting connection", e);
				}
			}
		}
	}


	public static void main(String[] args) {

		File dir;
		try{
			dir = new File(args[0]);
		} catch (ArrayIndexOutOfBoundsException ex) {
			System.out.println("Usage: java DirectoryServer rootDirectory port");
			return;
		}

		int port;
		try{
			port = Integer.parseInt(args[1]);
			if(port<=1024 || port>65535) port = 1999;
		} catch (RuntimeException ex) {
			port = 1999;
		}


		try {
			DirectoryServer dirserver = new DirectoryServer(dir, port);
			dirserver.start();
		} catch (IOException ex) {
			logger.log(Level.SEVERE,"Server could not start ", ex);
		}
	}
}