package downloader;

import java.net.URL;

public class Manager implements Runnable{

	URL url;
	Gui gui;
	String path, name, size, threads;
	
	public Manager(URL url, Gui gui, String path, String name, String size, String threads) {
		this.url = url;
		this.gui = gui;
		this.path = path;
		this.name = name;
		this.size = size;
		this.threads = threads;
	}
	
	public void run() {
		Download d1 = new Download(url, path, name, size, threads);
		Thread t1 = new Thread(d1);
		gui.addDownload(d1);
		t1.start();
	}
	
}
