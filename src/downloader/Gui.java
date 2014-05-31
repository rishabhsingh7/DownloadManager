package downloader;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

/*
 * Class to display Main download screen with list of files being downloaded.
 */
public class Gui extends JFrame{
	
	private static final long serialVersionUID = 1L;
	private JFrame mainFrame;
	private ArrayList<Download> totalDownloads = new ArrayList<Download>();
	public volatile ArrayList<Boolean> selectedFiles = new ArrayList<Boolean>();
	
	public Gui() {
		mainFrame = new JFrame();
		mainFrame.setTitle("Download Manager");
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setSize(d.width/2, d.height/2);
		mainFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		initUI();
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
		ActionListener refresh = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				initUI();
			}
		};
		Timer timer = new Timer(1000, refresh);	//Refreshing the GUI screen.
		timer.start();
	}
	
	public synchronized void addDownload(Download d) {
		totalDownloads.add(d);
	}
	
	public JFrame getFrame() {
		return mainFrame;
	}
	
	private void initUI() {
		//Menu Bar
		JMenuBar menuBar = new JMenuBar();
		ImageIcon icon = new ImageIcon("imgs/new.png");
		JMenuItem newItem = new JMenuItem("New", icon);
		newItem.setIconTextGap(10);
		newItem.setFont(new Font("SansSerif", Font.PLAIN, 18));
		newItem.setMaximumSize(new Dimension(100, 100));
		newItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				new Gui2(Gui.this);
			}
		});
		menuBar.add(newItem);
		
		icon = new ImageIcon("imgs/exit.png");
		JMenuItem exitItem = new JMenuItem("Exit", icon);
		exitItem.setIconTextGap(10);
		exitItem.setFont(new Font("SansSerif", Font.PLAIN, 18));
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				System.exit(0);
			}
		});
		menuBar.add(exitItem);
		mainFrame.setJMenuBar(menuBar);
		
		displayDownloads();
		
		mainFrame.revalidate();
		
	}
	
	//Display the files in table format.
	private void displayDownloads() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		JPanel headings = new JPanel();
		addHeading(headings, "");
		addHeading(headings, "File");
		addHeading(headings, "Size");
		addHeading(headings, "Progress");
		addHeading(headings, "Speed");
		addHeading(headings, "Threads");
		headings.setMaximumSize(new Dimension(900, 50));
		mainPanel.add(headings);
		for (int i = 1 ; i <= totalDownloads.size() ; i++) {
			JPanel rowPanel = new JPanel();
			addRow(rowPanel, i-1);
			rowPanel.setMaximumSize(new Dimension(900, 50));
			mainPanel.add(rowPanel);
			JSeparator line = new JSeparator();
			line.setMaximumSize(new Dimension(900, 1));
			mainPanel.add(line);
		}
		mainFrame.add(mainPanel);
	}
	
	private void addHeading(JPanel headings, String text) {
		JButton b1 = new JButton(text);
		b1.setContentAreaFilled(false);
		if (text.equals("")) {
			b1.setPreferredSize(new Dimension(80, 30));
			b1.setBorderPainted(false);
		}
		else if (text.equals("File"))
			b1.setPreferredSize(new Dimension(240, 30));
		else if (text.equals("Threads"))
			b1.setPreferredSize(new Dimension(100, 30));
		else
			b1.setPreferredSize(new Dimension(140, 30));
		b1.setFont(new Font("SansSerif", Font.BOLD, 14));
		headings.add(b1);
	}
	
	private void addRow(JPanel panel, final int i) {
		ImageIcon icon;
		if (totalDownloads.get(i).getStatus().equals("PAUSED"))
			icon = new ImageIcon("imgs/resume.png");
		else
			icon = new ImageIcon("imgs/pause.png");
		JButton pausePlay = new JButton(icon);
		pausePlay.setRolloverEnabled(false);
		pausePlay.setPreferredSize(new Dimension(40, 40));
		pausePlay.setContentAreaFilled(false);
		pausePlay.setBorderPainted(false);
		if (totalDownloads.get(i).getStatus().equals("COMPLETE") || totalDownloads.get(i).getStatus().equals("STOPPED"))
			pausePlay.setEnabled(false);
		else
			pausePlay.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (totalDownloads.get(i).getStatus().equals("DOWNLOADING")) {
						Thread t = new Thread(new Runnable() {
							public void run() {
								totalDownloads.get(i).pause();
							}
						});
						t.start();
				}
					else if (totalDownloads.get(i).getStatus().equals("PAUSED")) {
						Thread t = new Thread(new Runnable() {
							public void run() {
								totalDownloads.get(i).resume();
							}
						});
						t.start();
					}
				}
			});
		panel.add(pausePlay);
		
		if (totalDownloads.get(i).getStatus().equals("COMPLETE"))
			icon = new ImageIcon("imgs/complete.png");
		else
			icon = new ImageIcon("imgs/stop.png");
		JButton stop = new JButton(icon);
		stop.setRolloverEnabled(false);
		stop.setPreferredSize(new Dimension(40, 40));
		stop.setContentAreaFilled(false);
		stop.setBorderPainted(false);
		if (!totalDownloads.get(i).getStatus().equals("COMPLETE"))
			stop.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					Thread t = new Thread(new Runnable() {
						public void run() {
							totalDownloads.get(i).stop();
						}
					});
					t.start();
				}
			});
		panel.add(stop);
		
		JLabel nameLabel = new JLabel(totalDownloads.get(i).getName());
		nameLabel.setPreferredSize(new Dimension(240, 50));
		nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
		nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(nameLabel);
		
		JLabel sizeLabel = new JLabel(totalDownloads.get(i).getSize());
		sizeLabel.setPreferredSize(new Dimension(140, 50));
		sizeLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
		sizeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(sizeLabel);
		
		JProgressBar progBar = new JProgressBar(0, totalDownloads.get(i).getIntSize());
		progBar.setValue(totalDownloads.get(i).totalDownloaded.get());
		progBar.setStringPainted(true);
		progBar.setPreferredSize(new Dimension(140, 30));
		if (totalDownloads.get(i).getStatus().equals("PAUSED"))
			progBar.setForeground(Color.GRAY);
		else if (totalDownloads.get(i).getStatus().equals("STOPPED"))
			progBar.setForeground(Color.RED);
		panel.add(progBar);
		
		JLabel statusLabel = new JLabel(totalDownloads.get(i).getSpeed());
		statusLabel.setPreferredSize(new Dimension(140, 50));
		statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(statusLabel);
		
		JLabel threadLabel = new JLabel(totalDownloads.get(i).getNumThreads());
		threadLabel.setPreferredSize(new Dimension(100, 50));
		threadLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
		threadLabel.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(threadLabel);
	}
	
	public static void main(String[] args) {
		new Gui();
	}
}

/*
 * Input Screen used for adding a new file for downloading.
 */
class Gui2 extends JFrame{

	private static final long serialVersionUID = 1L;
	private JFrame mainFrame, newFrame;
	private JPanel mainPanel = new JPanel();
	Gui gui;
	
	public Gui2(Gui gui) {
		mainFrame = gui.getFrame();
		this.gui = gui;
		initUI();
	}
	
	private JFrame getFrame() {
		return newFrame;
	}
	
	private void initUI() {
		//Take URL as input from user.
		newFrame = new JFrame();
		newFrame.setTitle("Download New File");
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		newFrame.setSize(d.width/3, d.height/3);
		newFrame.setLocationRelativeTo(mainFrame);
		newFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		mainPanel.setLayout(new GridLayout(2, 1, 5, 5));
		JPanel addPanel = new JPanel();
		addPanel.setLayout(new GridLayout(3, 1, 5, 5));
		JPanel addPanel1 = new JPanel();
		final JTextField addField = new JTextField(30);
		addField.setFont(new Font("SansSerif", Font.PLAIN, 20));
		addPanel1.add(addField);
		JPanel addPanel3 = new JPanel();
		JLabel threadsLabel = new JLabel("Number of Threads: ");
		threadsLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
		final JTextField threadsField = new JTextField(2);
		threadsField.setFont(new Font("SansSerif", Font.PLAIN, 20));
		addPanel3.add(threadsLabel);
		addPanel3.add(threadsField);
		JPanel addPanel2 = new JPanel();
		ImageIcon icon = new ImageIcon("imgs/done.png");
		final JButton addButton = new JButton("Add URL", icon);
		addButton.setFont(new Font("SansSerif", Font.PLAIN, 18));
		addButton.setContentAreaFilled(false);
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String s = addField.getText();
				final String s2 = threadsField.getText();
				if (s.length() < 8 && (!s.substring(0, 7).equals("http://") || !s.substring(0, 7).equals("https://")))
					JOptionPane.showMessageDialog(getFrame(), "Invalid URL", "Error", JOptionPane.ERROR_MESSAGE);
				else{
					try {
						final URL url = new URL(s);
						addField.setEnabled(false);
						addButton.setEnabled(false);
						threadsField.setEnabled(false);
						Thread t = new Thread(new Runnable() {
							public void run() {
								updateUI(newFrame, url, s2);
							}
						});
						t.start();
					} catch (MalformedURLException e) {
						JOptionPane.showMessageDialog(getFrame(), "Invalid URL", "Error", JOptionPane.ERROR_MESSAGE);
						e.printStackTrace();
					}
				}
			}
		});
		addPanel2.add(addButton);
		addPanel.add(addPanel1);
		addPanel.add(addPanel3);
		addPanel.add(addPanel2);
		mainPanel.add(addPanel);
		newFrame.add(mainPanel);
		
		newFrame.setVisible(true);
	}
	
	//Display initial information about the URL and ask for File Save location.
	private void updateUI(final JFrame newFrame, final URL url, final String threads) {
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			final float size = conn.getContentLength() / (float)1000000;
			String s = url.getFile();
			final String fileName = s.substring(s.lastIndexOf('/') + 1);
			conn.disconnect();
			final DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(2);
			JPanel infoPanel = new JPanel();
			infoPanel.setLayout(new GridLayout(2, 1, 5, 5));
			JPanel infoPanel1 = new JPanel();
			JLabel nameLabel = new JLabel(fileName +" - ");
			nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
			JLabel sizeLabel = new JLabel("("+df.format(size)+" MB)");
			sizeLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
			infoPanel1.add(nameLabel);
			infoPanel1.add(sizeLabel);
			JPanel infoPanel2 = new JPanel();
			ImageIcon icon = new ImageIcon("imgs/download.png");
			JButton downloadButton = new JButton("Download", icon);
			downloadButton.setFont(new Font("SansSerif", Font.PLAIN, 18));
			downloadButton.setContentAreaFilled(false);
			downloadButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					JFileChooser chooser = new JFileChooser();
					chooser.setDialogType(JFileChooser.SAVE_DIALOG);
					String fileExt = fileName.substring(fileName.lastIndexOf('.')+1);
					FileNameExtensionFilter filter = new FileNameExtensionFilter("."+fileExt, fileExt);
					chooser.setFileFilter(filter);
				    chooser.setSelectedFile(new java.io.File(fileName));
					chooser.setDialogTitle("Save");
				    if (chooser.showSaveDialog(newFrame) == JFileChooser.APPROVE_OPTION) { 
				        gui.selectedFiles.add(false);
				        String s = chooser.getSelectedFile() +"";
				        if (s.lastIndexOf('.') == -1)
				        	s = chooser.getSelectedFile() +"."+ ((FileNameExtensionFilter)chooser.getFileFilter()).getExtensions()[0];
						new Thread(new Manager(url, gui, chooser.getCurrentDirectory()+"", s.substring(s.lastIndexOf('\\')+1), df.format(size)+" MB", threads)).start();
						newFrame.dispose();
				    }
				}
			});
			infoPanel2.add(downloadButton);
			icon = new ImageIcon("imgs/cancel.png");
			JButton cancelButton = new JButton("Cancel", icon);
			cancelButton.setFont(new Font("SansSerif", Font.PLAIN, 18));
			cancelButton.setContentAreaFilled(false);
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					newFrame.dispose();
				}
			});
			infoPanel2.add(cancelButton);
			infoPanel.add(infoPanel1);
			infoPanel.add(infoPanel2);
			mainPanel.add(infoPanel);
			newFrame.revalidate();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
