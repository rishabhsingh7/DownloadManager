package downloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Download implements Runnable{

	//Buffer size for getting the number of bytes from the server at a time
	public final int BUFFER_SIZE = 4096;
	AtomicBoolean paused = new AtomicBoolean(false);
	AtomicBoolean stopped = new AtomicBoolean(false);
	Lock lock = new ReentrantLock();
	Condition isResumed = lock.newCondition();
	//Number of threads that will download the parts simultaneously.
	private int numThreads = 32;
	
	//All threads will use this variable to write their downloaded bytes.
	public AtomicInteger totalDownloaded = new AtomicInteger(0);
	
	//Number of threads running with respect to this file being downloaded. Threads will increment and decrement this.
	public AtomicInteger runningThreads = new AtomicInteger(0);
	
	private URL url;
	private int totalSize = 0;
	public String path, name;
	private String initSize, status = "IDLE";
	private long startTime, pausedTime;
	
	public Download(URL url, String path, String name, String size, String threads) {
		this.url = url;
		this.name = name;
		initSize = size;
		this.path = path;
		startTime = System.currentTimeMillis();
		numThreads = Integer.parseInt(threads);
	}
	
	public String getName() {
		return name;
	}
	
	public int getIntSize() {
		return totalSize;
	}
	
	public String getSize() {
		return initSize;
	}
	
	public String getStatus() {
		return status;
	}
	
	//Calculate download speed according to the start time and bytes downloaded so far.
	public String getSpeed() {
		if (status.equals("DOWNLOADING")) {
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(1);
			float speed = (float)totalDownloaded.get() / (float)(System.currentTimeMillis() - startTime);
			if (speed > 1000)
				return df.format(speed/1000) +" MB/s";
			else
				return df.format(speed) +" KB/s";
		}
		else if (status.equals("COMPLETE"))
			return "Done!";
		else if (status.equals("FINISHING"))
			return "Finishing..";
		else if (status.equals("STOPPED"))
			return "Stopped.";
		else if (status.equals("PAUSED"))
			return "Paused";
		else if (status.equals("CONNECTING"))
			return "Connecting..";
		else
			return "";
	}
	
	public String getNumThreads() {
		return runningThreads.get()+"";
	}
	
	
	public void pause() {
		if (status.equals("DOWNLOADING")) {
			status = "PAUSED";
			paused.set(true);
			pausedTime = System.currentTimeMillis();
			for (int i = 0 ; i < numThreads ; i++) {
				runningThreads.getAndDecrement();
				
			}
		}
	}
	
	public void resume() {
		if (status.equals("PAUSED")) {
			status = "DOWNLOADING";
			paused.set(false);
			lock.lock();
			try{
				isResumed.signalAll();
			}finally{
				lock.unlock();
			}
			startTime = startTime + (System.currentTimeMillis() - pausedTime);
			for (int i = 0 ; i < numThreads ; i++) {
				runningThreads.getAndIncrement();
			}
		}
	}
	
	public void stop() {
		if (status.equals("DOWNLOADING") || status.equals("PAUSED")) {
			status = "STOPPED";
			stopped.set(true);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (int i = 1 ; i <= numThreads ; i++) {
				runningThreads.getAndDecrement();
				File f = new File(path +"\\"+ name +"."+ i);
				f.delete();
			}
		}
	}
	
	public void run() {
		try {
			runningThreads.getAndIncrement();
			status = "CONNECTING";
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			//Set Range property to check if server supports downloading in parts.
			conn.setRequestProperty("Range", "bytes="+ 0 +"-");
			int size = conn.getContentLength();
			totalSize = size;
			
			if (conn.getResponseCode() == 200) {
				/*
				 * SERVER DOES NOT SUPPORT DIVISION IN PARTS
				 */
				
				RandomAccessFile file = new RandomAccessFile(path+"\\"+name, "rw");
				InputStream stream = conn.getInputStream();
				int downloaded = 0;
				status = "DOWNLOADING";
				
				while (true) {
					if (stopped.get()) {
						file.close();
						return;
					}
					lock.lock();
					try{
					while(paused.get()){
						isResumed.await();
					}
					}finally{
						lock.unlock();
					}
					byte buffer[];
					if (size - downloaded > BUFFER_SIZE)
						buffer = new byte[BUFFER_SIZE];
					else
						buffer = new byte[size - downloaded];
					int read;
					if ((read = stream.read(buffer)) == -1)
						break;
					file.write(buffer, 0, read);
					downloaded += read;
					totalDownloaded.set(totalDownloaded.addAndGet(read));
				}
				stream.close();
				file.close();
				status = "COMPLETE";
				runningThreads.getAndDecrement();
			}
			
			else if (conn.getResponseCode() == 206){
				/*
				 * SERVER SUPPORTS DIVISION IN PARTS
				 */
				
				conn = (HttpURLConnection) url.openConnection();
				int temp = size / numThreads - 1;
				conn.setRequestProperty("Range", "bytes="+ 0 +"-"+ temp);
				RandomAccessFile file;
				if (numThreads != 1)
					file = new RandomAccessFile(path +"\\"+ name +".1", "rw");
				else
					file = new RandomAccessFile(path +"\\"+ name, "rw");
				InputStream stream = conn.getInputStream();
				ArrayList<Thread> threads = new ArrayList<Thread>();
				
				//Distribute file size among 3 threads, one of which is this thread itself
				for (int i = 1 ; i < numThreads ; i++) {
					int start = (size / numThreads) * i;
					int end = ((size / numThreads) * i) + (size / numThreads) - 1;
					if (i + 1 != numThreads) {
						Thread t = new Thread(new PartDownloader(url, start, end, file, this, i+1));
						threads.add(t);
					}
					else {
						Thread t = new Thread(new PartDownloader(url, start, size, file, this, i+1));
						threads.add(t);
					}
				}
				status = "DOWNLOADING";
				for (Thread t : threads) {
					runningThreads.getAndIncrement();
					t.start();
				}
				
				//This thread will download the first part
				file.seek(0);
				int downloaded = 0;
				
				while (true) {
					if (stopped.get()) {
						file.close();
						return;
					}
					lock.lock();
					try{
					while(paused.get()){
						isResumed.await();
					}
					}finally{
						lock.unlock();
					}
					byte buffer[];
					if (size - downloaded > BUFFER_SIZE)
						buffer = new byte[BUFFER_SIZE];
					else
						buffer = new byte[size - downloaded];
					int read;
					if ((read = stream.read(buffer)) == -1)
						break;
					file.write(buffer, 0, read);
					downloaded += read;
					totalDownloaded.set(totalDownloaded.addAndGet(read));
				}
				for (Thread t : threads)
					t.join();
				stream.close();
				file.close();
				status = "FINISHING";
				
				/*
				 * Combine the generated output file parts into one and delete those parts.
				 */
				if (numThreads > 1) {
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(path +"\\"+ name), false));
					for (int i = 1 ; i <= numThreads ; i++) {
						File f = new File(path +"\\"+ name +"."+ i);
						BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
						int value;
						while ((value = bis.read()) != -1) {
							bos.write(value);
						}
						bos.flush();
						bis.close();
						f.delete();
					}
					bos.close();
				}
				status = "COMPLETE";
				System.out.println(name +": "+ (System.currentTimeMillis() - startTime));
				runningThreads.getAndDecrement();
			}
			else {
				System.err.println("ERROR: Response Code not 2xx");
				System.exit(0);
			}
			
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}

/*
 * This class is used to  generate a number of threads, each of which will download a part of file to be downloaded.
 */
class PartDownloader implements Runnable {
	private int BUFFER_SIZE;
	URL url;
	int start, end, id;
	RandomAccessFile file;
	Download dwnld;
	
	PartDownloader(URL url, int start, int end, RandomAccessFile file, Download dwnld, int id) {
		this.url = url;
		this.start = start;
		this.end = end;
		this.file = file;
		this.dwnld = dwnld;
		this.id = id;
		BUFFER_SIZE = dwnld.BUFFER_SIZE;
	}
	
	public void run() {
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Range", "bytes="+ start +"-"+ end);
			conn.connect();
			int size = conn.getContentLength();
			
			//Add .ID_NUMBER after file name for in order joining later to get final file.
			RandomAccessFile file = new RandomAccessFile(dwnld.path +"\\"+ dwnld.name +"."+ id, "rw");
			InputStream stream = conn.getInputStream();
			int downloaded = 0;
			
			while (true) {
				if (dwnld.stopped.get()) {
					file.close();
					return;
				}
				dwnld.lock.lock();
				try{
				while(dwnld.paused.get()){
					try {
						dwnld.isResumed.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				}finally{
					dwnld.lock.unlock();
				}
				byte buffer[];
				if (size - downloaded > BUFFER_SIZE)
					buffer = new byte[BUFFER_SIZE];
				else
					buffer = new byte[size - downloaded];
				int read;
				if ((read = stream.read(buffer)) == -1)
					break;
				file.write(buffer, 0, read);
				downloaded += read;
				dwnld.totalDownloaded.set(dwnld.totalDownloaded.addAndGet(read));
			}
			stream.close();
			file.close();
			dwnld.runningThreads.getAndDecrement();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
