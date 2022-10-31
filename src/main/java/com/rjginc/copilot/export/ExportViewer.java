package com.rjginc.copilot.export;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rjginc.copilot.copilot_simulator.JobDataCache;
import com.rjginc.copilot.copilot_simulator.JobDataCacheService;
import com.rjginc.esm.core.model.job.Cycle;
import com.rjginc.esm.core.model.job.cycle.EncodedCycleData;
import com.rjginc.esm.persistance.ArchivalHelper;
import com.rjginc.esm.persistance.IEWrapper;

import net.miginfocom.swing.MigLayout;
import javax.swing.JTextField;
import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;

public class ExportViewer extends JFrame
{
	ApplicationContext applicationContext;
	private JPanel contentPane;
	private JTextField txtdtestingcopilotrectrjg;
	private JTextPane textPane;
	
	@Autowired
	private JobDataCacheService jobDataCacheService;
	@Autowired
	ObjectMapper objectMapper;
	
	private JComboBox<String> comboBox;
	private JobDataCache jobDataCache = null;
	private JButton btnLoad;
	
	JFileChooser browser = new JFileChooser();
	
	@Inject
	ArchivalHelper archivalHelper;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					try
					{
						ExportViewer frame = new ExportViewer();
						frame.setVisible(true);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			});
	}

	/**
	 * Create the frame.
	 */
	public ExportViewer()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new MigLayout("", "[grow]", "[][][grow]"));
		
		txtdtestingcopilotrectrjg = new JTextField();
		txtdtestingcopilotrectrjg.setText("D:\\testing\\copilot\\4RECT.rjg");
		contentPane.add(txtdtestingcopilotrectrjg, "flowx,cell 0 0,growx");
		txtdtestingcopilotrectrjg.setColumns(10);
		
		new  FileDrop( txtdtestingcopilotrectrjg, new FileDrop.Listener()
	      {   public void  filesDropped( java.io.File[] files )
	          {   
	              if (files.length == 1)
	              {
	            	  txtdtestingcopilotrectrjg.setText(files[0].getPath());
	              }
	          }   // end filesDropped
	      }); 
		
		browser.setDialogTitle("Select Job Export");
		browser.setDialogType(JFileChooser.OPEN_DIALOG);
		browser.setFileFilter(new FileFilter()
			{
				
				@Override
				public String getDescription()
				{
					return "RJG Export";
				}
				
				@Override
				public boolean accept(File f)
				{
					return f.isDirectory() || f.getName().endsWith(".rjg");
				}
			});
		
		
		JButton btnBrowse = new JButton("Browse");
		btnBrowse.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (browser.showOpenDialog(ExportViewer.this) == JFileChooser.APPROVE_OPTION)
				{
					txtdtestingcopilotrectrjg.setText(browser.getSelectedFile().getAbsolutePath());
				}
			}
		});
		contentPane.add(btnBrowse, "cell 0 0");
		
		comboBox = new JComboBox<>();
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = (String) comboBox.getSelectedItem();
				if (name == null)
				{
					return;
				}
				try
				{
					if (name.contains("SummaryData"))
					{
						Float[] fs = jobDataCache.getSummaryData().get(name.replace("SummaryData/", ""));
						textPane.setText( Arrays.toString(fs));
					}
					else if (name.contains("CycleData") )
					{
						name = name.replace("/","_");
						int index = Cycle.getCycleIndex(name);
						EncodedCycleData encodedCycleData = jobDataCache.getCycleData(index );
						String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(encodedCycleData );
						textPane.setText(json);
					}
					else
					{		
						IEWrapper jobObject = jobDataCache.getJobObject(name);
						String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jobObject);						
						textPane.setText(json);
					}
				}
				catch (IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		comboBox.setEditable(true);
		contentPane.add(comboBox, "cell 0 1,growx");
		
		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, "cell 0 2,grow");
		
		textPane = new JTextPane();
		scrollPane.setViewportView(textPane);
		
		btnLoad = new JButton("Load");
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try
				{
					jobDataCache = jobDataCacheService.getJobDataCache( txtdtestingcopilotrectrjg.getText() );
					comboBox.removeAllItems();
					for ( String s : jobDataCache.getDocuments())
					{						
						comboBox.addItem( s );						
					}
					comboBox.setSelectedItem("Job");				
				}
				catch (IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
			}
		});
		contentPane.add(btnLoad, "cell 0 0");
		
		init();
	}

	private void init()
	{
		System.setProperty("spring.profiles.active", "viewer");
		applicationContext = new AnnotationConfigApplicationContext(ExportViewerConfig.class);	
		AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
		factory.autowireBean(this);
		System.out.println("Started");
//		factory.initializeBean(this,"frame");
//		jobDataCacheService = ctx.getBean(JobDataCacheService.class);
	}

}
