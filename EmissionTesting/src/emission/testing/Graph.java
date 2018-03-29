/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emission.testing;

//import gnu.io.SerialPort;
import com.fazecast.jSerialComm.*;
import static emission.testing.MainFrame.getCurrentTimeStamp;
import java.awt.BorderLayout;
import static java.awt.SystemColor.window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

import net.sf.jasperreports.view.JasperViewer;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;

/**
 *
 * @author genwockz
 */
public class Graph extends javax.swing.JFrame {
                 static SerialPort chosenPort;
                 static int x = 0;
                 static int y = 0;
                 
                 float  threshCO = 0;
                 float threshHC = 0;
                 int date_to = 0;
                 int date_from = 0;
                 
                 float  FthreshCO = 0;
                 float FthreshHC = 0;
                 int Fdate_to = 0;
                 int Fdate_from = 0;
                 
                 
                 int motion = 0 ;
                 int activate = 0;
                 XYSeries series = new XYSeries("Carbon monoxide Readings");
                 XYSeriesCollection dataset = new XYSeriesCollection(series);
                 XYSeries series1 = new XYSeries("HydroCarbon Readings");
                 XYSeriesCollection dataset1 = new XYSeriesCollection(series1);
                 SerialPort[] portNames = SerialPort.getCommPorts();
    /**
     * Creates new form Graph
     */
      static
  {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
  }

  private Boolean begin = false;
  private Boolean firstFrame = true;
  private VideoCapture video = null;
  private CaptureThread thread = null;
  private MatOfByte matOfByte = new MatOfByte();
  private BufferedImage bufImage = null;
  private InputStream in;
  private Mat frameaux = new Mat();
  private Mat frame = new Mat(240, 320, CvType.CV_8UC3);
  private Mat lastFrame = new Mat(240, 320, CvType.CV_8UC3);
  private Mat currentFrame = new Mat(240, 320, CvType.CV_8UC3);
  private Mat processedFrame = new Mat(240, 320, CvType.CV_8UC3);
  private ImagePanel image;
  private BackgroundSubtractorMOG bsMOG = new BackgroundSubtractorMOG();
  private int savedelay = 0;
  String remoteName="";
  String remoteName1="";
  String currentDir = "";
  String detectionsDir = "detections";         
 String passwordOverride = "c@rc3d0@dm!n"  ;
      String filename="";           
        String filename1="";          
        int flag=0;
        int yearmodel = 0;
        
                 
    public Graph() {
        
        initComponents();
        jCheckBoxAlarm.setVisible(false);
        jCheckBoxMotionDetection.setVisible(false);
//        jLabel1.setVisible(false);
//        jLabel4.setVisible(false);
//        jSliderSensibility.setVisible(false);
//        jSliderThreshold.setVisible(false);
        
        image = new ImagePanel(new ImageIcon("figs/320x240.gif").getImage());
    jPanelSource1.add(image, BorderLayout.CENTER);

    currentDir = Paths.get(".").toAbsolutePath().normalize().toString();
      System.out.println(currentDir);
    detectionsDir = currentDir + File.separator + detectionsDir;
      System.out.println(detectionsDir);
 
        populate_combo();
        create_graph();
       // jButton1.setEnabled(false);
        send.setEnabled(false);
        send1.setEnabled(false);
        
         try {
            DBconnection.init();
            Connection Myconnection = DBconnection.getConnection();
            PreparedStatement ps;
            ResultSet rs;
            
            ps = Myconnection.prepareStatement("truncate desipcdf_ltoemission.localjasper;");
            ps.execute();

        } catch (SQLException ex) {
            Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
        }
      
        
    }
    
    public void getValues(){
        
        try {
            DBconnection.init();
            Connection Myconnection = DBconnection.getConnection();
            PreparedStatement ps;
            ResultSet rs;
            ps = Myconnection.prepareStatement("SELECT * FROM desipcdf_ltoemission.emission_standards where criteria = '"+this.classif.getText()+"'");
            rs = ps.executeQuery();
            while (rs.next()) {
                
               date_from = Integer.parseInt(rs.getString(3));
               date_to = Integer.parseInt(rs.getString(4));
               yearmodel = Integer.parseInt(year.getText());
               
               if(date_from<=yearmodel && yearmodel<=date_to){
                     FthreshCO =Float.parseFloat(rs.getString(5)); 
                     FthreshHC = Float.parseFloat(rs.getString(6)); 
                     Fdate_to = Integer.parseInt(rs.getString(4));
                     Fdate_from = Integer.parseInt(rs.getString(3));
                     String a = String.valueOf(FthreshCO);
                     String b = String.valueOf(FthreshHC);
                   passingCO.setText("passing:"+a);
                    passingHC.setText("passing:"+b);
               }
                
                
            } 
        } catch (SQLException e) {
            System.out.println(e);
        }
        
        
        
        
        
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panel = new javax.swing.JPanel();
        connectButton = new javax.swing.JButton();
        portList = new javax.swing.JComboBox<>();
        refresh = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        information = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        status = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        ppm = new javax.swing.JLabel();
        raw = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        passingCO = new javax.swing.JLabel();
        send = new javax.swing.JButton();
        panel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        ppm1 = new javax.swing.JLabel();
        raw1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        passingHC = new javax.swing.JLabel();
        jPanelSource1 = new javax.swing.JPanel();
        jLabelSource1 = new javax.swing.JLabel();
        jTextFieldSource1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        Capture = new javax.swing.JButton();
        jButtonStart = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jSliderSensibility = new javax.swing.JSlider();
        jSliderThreshold = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jCheckBoxAlarm = new javax.swing.JCheckBox();
        jCheckBoxMotionDetection = new javax.swing.JCheckBox();
        pas = new javax.swing.JPasswordField();
        jLabel6 = new javax.swing.JLabel();
        ok = new javax.swing.JButton();
        send1 = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        hcresult = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        coresult = new javax.swing.JLabel();
        year = new javax.swing.JLabel();
        classif = new javax.swing.JLabel();
        jButtonStart1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBounds(new java.awt.Rectangle(6, 65, 1360, 600));
        setMaximumSize(new java.awt.Dimension(1360, 600));
        setMinimumSize(new java.awt.Dimension(1360, 600));
        setPreferredSize(new java.awt.Dimension(1360, 600));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        panel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panel.setLayout(new java.awt.BorderLayout());

        connectButton.setText("Connect");
        connectButton.setEnabled(false);
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });

        refresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/refresh.png"))); // NOI18N
        refresh.setText("refresh");
        refresh.setPreferredSize(new java.awt.Dimension(35, 35));
        refresh.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                refreshMouseClicked(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Chassis Number"));

        information.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        information.setText("information");
        information.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(information, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(information)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Sensor Status", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        status.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        status.setText("Sensor Status");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(status)
                .addGap(0, 13, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Carbon Monoxide Readings", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        ppm.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ppm.setText("0%");
        ppm.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        raw.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        raw.setText("0");
        raw.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        jLabel3.setText("Raw Value:");

        passingCO.setText(" ");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ppm, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE)
                .addComponent(passingCO, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(raw, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ppm)
                    .addComponent(raw)
                    .addComponent(jLabel3)
                    .addComponent(passingCO))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        send.setText("SEND");
        send.setEnabled(false);
        send.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendActionPerformed(evt);
            }
        });

        panel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panel1.setLayout(new java.awt.BorderLayout());

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "HydroCarbon Readings", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        ppm1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ppm1.setText("0 PPM");
        ppm1.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        raw1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        raw1.setText("0");
        raw1.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        jLabel5.setText("Raw Value:");

        passingHC.setText(" ");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ppm1, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 41, Short.MAX_VALUE)
                .addComponent(passingHC, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(raw1, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ppm1)
                    .addComponent(raw1)
                    .addComponent(jLabel5)
                    .addComponent(passingHC))
                .addGap(0, 13, Short.MAX_VALUE))
        );

        jPanelSource1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout jPanelSource1Layout = new javax.swing.GroupLayout(jPanelSource1);
        jPanelSource1.setLayout(jPanelSource1Layout);
        jPanelSource1Layout.setHorizontalGroup(
            jPanelSource1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 320, Short.MAX_VALUE)
        );
        jPanelSource1Layout.setVerticalGroup(
            jPanelSource1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 240, Short.MAX_VALUE)
        );

        jLabelSource1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelSource1.setText("Source 1:");
        jLabelSource1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabelSource1MouseClicked(evt);
            }
        });
        jLabelSource1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jLabelSource1KeyPressed(evt);
            }
        });

        jTextFieldSource1.setText("1");

        jLabel2.setText("(zero for local webcamera)");

        Capture.setText("Capture");
        Capture.setEnabled(false);
        Capture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CaptureActionPerformed(evt);
            }
        });

        jButtonStart.setText("Start");
        jButtonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStartActionPerformed(evt);
            }
        });

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Administrative Tools"));

        jSliderSensibility.setMinimum(1);
        jSliderSensibility.setPaintLabels(true);
        jSliderSensibility.setPaintTicks(true);
        jSliderSensibility.setValue(10);
        jSliderSensibility.setEnabled(false);

        jSliderThreshold.setMaximum(255);
        jSliderThreshold.setPaintLabels(true);
        jSliderThreshold.setPaintTicks(true);
        jSliderThreshold.setValue(15);
        jSliderThreshold.setEnabled(false);

        jLabel1.setText("Threshold:");

        jLabel4.setText("Sensibility:");

        jCheckBoxAlarm.setText("Alarm");
        jCheckBoxAlarm.setEnabled(false);

        jCheckBoxMotionDetection.setText("Motion Detection");
        jCheckBoxMotionDetection.setEnabled(false);

        pas.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        pas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasActionPerformed(evt);
            }
        });

        jLabel6.setText("Password:");

        ok.setText("OK");
        ok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jCheckBoxMotionDetection)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jCheckBoxAlarm)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel4)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSliderThreshold, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
                    .addComponent(jSliderSensibility, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pas)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ok))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(ok))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSliderThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jCheckBoxMotionDetection)
                        .addComponent(jLabel1)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSliderSensibility, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jCheckBoxAlarm)))
        );

        send1.setText("PRINT");
        send1.setEnabled(false);
        send1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                send1ActionPerformed(evt);
            }
        });

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Test Results", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "HydroCarbon", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        hcresult.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        hcresult.setText(" ");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(hcresult, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(hcresult)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Carbon Monoxide", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        coresult.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        coresult.setText(" ");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(coresult, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(coresult)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        year.setText("  ");

        classif.setText("  ");

        jButtonStart1.setText("Stop");
        jButtonStart1.setEnabled(false);
        jButtonStart1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStart1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(41, 41, 41)
                                        .addComponent(refresh, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(portList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(year)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(classif)))
                                .addGap(11, 11, 11)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(send1, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                                    .addComponent(connectButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(10, 10, 10)
                                .addComponent(send, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(6, 6, 6)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(Capture)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButtonStart1, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonStart, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelSource1)
                                .addGap(4, 4, 4)
                                .addComponent(jTextFieldSource1, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(4, 4, 4)
                                .addComponent(jLabel2))
                            .addComponent(jPanelSource1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(10, 10, 10)
                        .addComponent(panel, javax.swing.GroupLayout.PREFERRED_SIZE, 486, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10)
                        .addComponent(panel1, javax.swing.GroupLayout.PREFERRED_SIZE, 486, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldSource1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(3, 3, 3)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabelSource1)
                                    .addComponent(jLabel2))))
                        .addGap(11, 11, 11)
                        .addComponent(jPanelSource1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(Capture)
                            .addComponent(jButtonStart)
                            .addComponent(jButtonStart1))
                        .addGap(24, 24, 24))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(32, 32, 32)
                                        .addComponent(portList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(send)
                                        .addComponent(send1))
                                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(20, 20, 20)
                                        .addComponent(refresh, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(49, 49, 49)
                                .addComponent(connectButton))))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(layout.createSequentialGroup()
                            .addGap(0, 17, Short.MAX_VALUE)
                            .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(year)
                                        .addComponent(classif))
                                    .addGap(0, 0, Short.MAX_VALUE)))
                            .addContainerGap()))))
        );

        year.setVisible(false);
        classif.setVisible(false);

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
           getValues();
        connectButton.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent arg0) {
				if(connectButton.getText().equals("Connect")) {
					// attempt to connect to the serial port
					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					if(chosenPort.openPort()) {
						//connectButton.setText("Disconnect");
                                                //jButton1.setEnabled(true);
                                                connectButton.setEnabled(false);
						portList.setEnabled(false);
					}
					
					// create a new thread that listens for incoming text and populates the graph
					Thread thread = new Thread(){
						@Override public void run() {
                                                    activate = 1;
							Scanner scanner = new Scanner(chosenPort.getInputStream());
                                                        float percentage = 0;
                                                        float average = 0;
                                                        float sumHC = 0;
                                                        float sumCO = 0;
                                                        float sumLPG = 0;
                                                        float sumSMOKE = 0;
                                                        int count=1;
							while(scanner.hasNextLine()) {
								try {
                                                                    if(motion == 1){
                                                                        
                                                                        getThread().interrupt();
                                                                        
                                                                    }
                                                                   
                                                                    
                                                                    
									String line = scanner.nextLine();
                                                                        String[] token = line.split(" ");
                                                                        if(token[0].equals("adjusting")){
                                                                            System.out.println(line);
                                                                            status.setText(line);
                                                                           
                                                                         }else{
                                                                           
                                                                             System.out.println(token[0]);
                                                                        System.out.println(token[1]);
                                                                         System.out.println(token[2]);
                                                                          System.out.println(token[3]);
                                                                         float number = Float.parseFloat(token[0]);
                                                                         series.add(x++, number);
                                                                         float w = Float.parseFloat(token[1]);
                                                                         
                                                                         
                                                                       //  ppm.setText(String.valueOf(percentage) + "%");
                                        float rawHC = Float.parseFloat(token[6]);
                                        float LPG = Float.parseFloat(token[8]);
                                        float CO = Float.parseFloat(token[11]);                                 
                                        float SMOKE = Float.parseFloat(token[14]);       
                                        System.out.println(LPG);
                                        System.out.println(CO);
                                        System.out.println(SMOKE);
                                                                        series1.add(y++, rawHC);
                                                                        
                                                                        sumHC += LPG;
                                                                        sumSMOKE += SMOKE;
                                                                        sumCO += CO;
                                                                        sumLPG += SMOKE;
                                                                        
                                                                        
//                                                                            System.out.println("HC" + (sumHC/count));
//                                                                            System.out.println("LPG" + (sumHC/count));
//                                                                            System.out.println("CO" + (sumHC/count));
                                                                        // ppm1.setText(String.valueOf(sumHC) + "PPM");
                                                                        
                                                                      
                                                                        
                                                                          if(w<1000){
                                                                           // percentage =(float) ((w/10000) + 0.3);
                                                                        percentage =(float) ((w/10000));
                                                                            System.out.println("wala kaabot ug 1000");
                                                                            ppm.setText(String.valueOf(percentage) + "%");
                                                                        }else{
                                                                        percentage =(float) ((w/10000) + 0.3);
                                                                           //  percentage =(float) ((w/10000));
                                                                          System.out.println("nalapas ug 1000");
                                                                          ppm.setText(String.valueOf(percentage) + "%");
                                                                        } 
                                                                         
                                                                        count++;
                                                                        if(count>=90){
                                                                            System.out.println("SMOKE" + (sumSMOKE/count));
                                                                            System.out.println("LPG" + (sumLPG/count));
                                                                            System.out.println("CO" + (sumCO/count));
                                                                  
                                                                            if((sumSMOKE/count)<= 0){
                                                                                 average = (sumSMOKE/count);
                                                                                 System.out.println("0 value");
                                                                                }else{
                                                                                 average = 50+(sumSMOKE/count);
                                                                                 System.out.println("plusan 50");
                                                                                }
                                                                            
                                                                            
                                                                     
                                                                         ppm1.setText(String.valueOf(average) + "PPM");
                                                                         
                                                                         if(FthreshCO>=percentage){
                                                                            coresult.setText("PASS");
                                                                         }else{
                                                                            coresult.setText("FAILED");
                                                                         }
                                                                         
                                                                         if(FthreshHC>=average){
                                                                            hcresult.setText("PASS");
                                                                         }else{
                                                                             hcresult.setText("FAILED");
                                                                         }
                                                                         
                                                                         count=0;
                                                                        }
                                                                        
                                                        
                                                                         if(token[2].equals("0.10")){
                                                                             status.setText("Measurement Phase");
                                                                             
                                                                             
                                                                         }else{
                                                                             if(flag==0){
                                                                             status.setText("Heating Phase");
                                                                             captureLast();
                                                                             try{ Thread.sleep(1500); } catch(Exception ex){}
                                                                             send.setEnabled(true);
                                                                             send1.setEnabled(true);
                                                                             flag=1;
                                                                             }else{
                                                                             status.setText("Heating Phase");
                                                                             }
                                                                            // disconnect();
                                                                         }
                                                                         raw1.setText(token[6]);
                                                                         raw.setText(token[0]);
                                                                         panel.repaint();
                                                                         panel1.repaint();
                                                                        }
                                                                       
									int number = Integer.parseInt(line);
                                                                        
									
									
								} catch(Exception e) {System.out.println(e);}
							}
							scanner.close();
						}
					};
					thread.start();
				} else {
					// disconnect from the serial port
					chosenPort.closePort();
					portList.setEnabled(true);
					connectButton.setEnabled(true);
					series.clear();
					x = 0;
				}
			}
		});
        
        
    }//GEN-LAST:event_connectButtonActionPerformed

    
    public Thread getThread(){
        
        
        this.dispose();
           delDir();
          disconnect();
          stop();
          EnterInfo a = new EnterInfo();
          a.show();
                                return thread;
    }
    
    
    
    
    private void refreshMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_refreshMouseClicked
        // TODO add your handling code here:
        System.out.println(getBounds());
        populate_combo();
    }//GEN-LAST:event_refreshMouseClicked

    private void start()
  {
    //System.out.println("You clicked the start button!");

    if(!begin)
    {
      int sourcen = Integer.parseInt(jTextFieldSource1.getText());
      System.out.println("Opening source: " + sourcen);

      video = new VideoCapture(sourcen);

      if(video.isOpened())
      {
        thread = new CaptureThread();
        thread.start();
        begin = true;
        firstFrame = true;
      }
    }
  }
    
    
  private void stop()
  {
    //System.out.println("You clicked the stop button!");

    if(begin)
    {
      System.out.println("You clicked the stop button!");
    begin = false;
    try{ Thread.sleep(1000); } catch(Exception ex){}
    video.release();
    this.dispose();
    }
  }

  public static String getCurrentTimeStamp()
  {
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");//dd/MM/yyyy
    Date now = new Date();
    String strDate = sdfDate.format(now);
    return strDate;
  }

  public ArrayList<Rect> detection_contours(Mat frame, Mat outmat)
  {
    Mat v = new Mat();
    Mat vv = outmat.clone();
    List<MatOfPoint> contours = new ArrayList();
    Imgproc.findContours(vv, contours, v, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

    double maxArea = 100;
    int maxAreaIdx;
    Rect r;
    ArrayList<Rect> rect_array = new ArrayList();

    for(int idx = 0; idx < contours.size(); idx++)
    {
      Mat contour = contours.get(idx);
      double contourarea = Imgproc.contourArea(contour);
      if(contourarea > maxArea)
      {
        // maxArea = contourarea;
        maxAreaIdx = idx;
        r = Imgproc.boundingRect(contours.get(maxAreaIdx));
        rect_array.add(r);
        Imgproc.drawContours(frame, contours, maxAreaIdx, new Scalar(0, 0, 255));
      }
    }

    v.release();
    return rect_array;
  }

  class CaptureThread extends Thread
  {
    @Override
    public void run()
    {
      if(video.isOpened())
      {
        while(begin == true)
        {
          //video.read(frameaux);
          video.retrieve(frameaux);
          Imgproc.resize(frameaux, frame, frame.size());
          frame.copyTo(currentFrame);
          
          if(firstFrame)
          {
            frame.copyTo(lastFrame);
            firstFrame = false;
            continue;
          }

          if(jCheckBoxMotionDetection.isSelected())
          {
            Imgproc.GaussianBlur(currentFrame, currentFrame, new Size(3, 3), 0);
            Imgproc.GaussianBlur(lastFrame, lastFrame, new Size(3, 3), 0);
            
            //bsMOG.apply(frame, processedFrame, 0.005);
            Core.subtract(currentFrame, lastFrame, processedFrame);
            //Core.absdiff(frame,lastFrame,processedFrame);
            
            Imgproc.cvtColor(processedFrame, processedFrame, Imgproc.COLOR_RGB2GRAY);
            //
            
            int threshold = jSliderThreshold.getValue();
            //Imgproc.adaptiveThreshold(processedFrame, processedFrame, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 5, 2);
            Imgproc.threshold(processedFrame, processedFrame, threshold, 255, Imgproc.THRESH_BINARY);

            ArrayList<Rect> array = detection_contours(currentFrame, processedFrame);
            ///*
            if(array.size() > 0)
            {
              Iterator<Rect> it2 = array.iterator();
              while(it2.hasNext())
              {
                Rect obj = it2.next();
                Core.rectangle(currentFrame, obj.br(), obj.tl(),
                  new Scalar(0, 255, 0), 1);
              }
            }
            //*/
            
            if(jCheckBoxAlarm.isSelected())
            {
              double sensibility = jSliderSensibility.getValue();
              //System.out.println(sensibility);
              double nonZeroPixels = Core.countNonZero(processedFrame);
             // System.out.println("nonZeroPixels: " + nonZeroPixels);

              double nrows = processedFrame.rows();
              double ncols = processedFrame.cols();
              double total = nrows * ncols / 10;

              double detections = (nonZeroPixels / total) * 100;
              //System.out.println(detections);
              if(detections >= sensibility)
              {
                  
                //System.out.println("ALARM ENABLED!");
                Core.putText(currentFrame, "MOTION DETECTED", 
                  new Point(5,currentFrame.cols()/2), //currentFrame.rows()/2 currentFrame.cols()/2
                  Core.FONT_HERSHEY_TRIPLEX , new Double(1), new Scalar(0,0,255));
                if(activate == 1){
                motion = 1;
                }
                
                //diri ibutang
               
             
              
                
                
                
                
//                if(jCheckBoxSave.isSelected())
//                {
//                  if(savedelay == 2)
//                  {
//                    String filename = jTextFieldSaveLocation.getText() + File.separator + "capture_" + getCurrentTimeStamp() + ".jpg";
//                    System.out.println("Saving results in: " + filename);
//                    Highgui.imwrite(filename, frame);
//                    savedelay = 0;
//                      System.out.println("savedelay if");
//                  }
//                  else{
//                  
//                   savedelay = savedelay + 1;
//                  System.out.println("savedelay else");
//                  }
//                   
//                }
              }
              else
              {
                savedelay = 0;
                //System.out.println("savedelay else sa tuncheck");
                //System.out.println("");
              }
            }
            //currentFrame.copyTo(processedFrame);
          }
          else
          {
            //frame.copyTo(processedFrame);
          }
          
          currentFrame.copyTo(processedFrame);

          Highgui.imencode(".jpg", processedFrame, matOfByte);
          byte[] byteArray = matOfByte.toArray();

          try
          {
            in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
          }
          catch(Exception ex)
          {
            ex.printStackTrace();
          }

          //image.updateImage(new ImageIcon("figs/lena.png").getImage());
          image.updateImage(bufImage);

          frame.copyTo(lastFrame);

          try
          {
            Thread.sleep(1);
          }
          catch(Exception ex)
          {
          }
        }
      }
    }
  }
    
    public void snap(){
     JOptionPane.showMessageDialog(this, "Snap a motion has been detected, This for security purposes. The procedure will now end and you need to retake again!", "Error", JOptionPane.INFORMATION_MESSAGE);
    
    }
    
    private void CaptureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CaptureActionPerformed
        // TODO add your handling code here:

        captureFirst();
         try
          {
            Thread.sleep(5000);
          }
          catch(Exception ex)
          {
          }
         jCheckBoxAlarm.setSelected(true);
         jCheckBoxMotionDetection.setSelected(true);
         connectButton.setEnabled(true);
         Capture.setEnabled(false);
    }//GEN-LAST:event_CaptureActionPerformed

    public void captureFirst(){
    
      filename = detectionsDir + File.separator +information.getText()+ "_F_" + getCurrentTimeStamp() + ".jpg";
     
      remoteName = information.getText()+ "_F_" + getCurrentTimeStamp() + ".jpg";
                    System.out.println("Saving results in: " + filename);
                    Highgui.imwrite(filename, frame);
                    System.out.println("saved");
    
    
    }
    
    

    
    public void captureLast(){
    
       filename1 = detectionsDir + File.separator +information.getText()+ "_L_" + getCurrentTimeStamp() + ".jpg";
       remoteName1 = information.getText()+ "_L_" + getCurrentTimeStamp() + ".jpg";
                    System.out.println("Saving results in: " + filename1);
                    Highgui.imwrite(filename1, frame);
                    System.out.println("saved");
                    
                    String success="CO:"+coresult.getText()+" HC:"+hcresult.getText();
                    String co=ppm.getText()+" %";
                    String hc=ppm1.getText()+" PPM";
                  try {
            DBconnection.init();
            Connection Myconnection = DBconnection.getConnection();
            PreparedStatement ps;
            ResultSet rs;
            ps = Myconnection.prepareStatement("Insert into localjasper(Chassis, co, hc, success)values('"+information.getText()+"','"+co+"','"+hc+"','"+success+"')");
            ps.execute();
                      System.out.println("save to local drive");
        } catch (SQLException ex) {
            Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
        }   
                    
    
    
    }
    
    
    private void jButtonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStartActionPerformed
       Capture.setEnabled(true);
      jButtonStart1.setEnabled(true);
       jButtonStart.setEnabled(false);
        start();
        
    }//GEN-LAST:event_jButtonStartActionPerformed

    private void pasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasActionPerformed
        // TODO add your handling code here:
        
    }//GEN-LAST:event_pasActionPerformed

    private void okActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okActionPerformed
        // TODO add your handling code here:
        if(ok.getText().equals("OK")){
        if(pas.getText().equals(passwordOverride)){
        jSliderSensibility.setVisible(true);
        jSliderThreshold.setVisible(true);
       jSliderSensibility.setEnabled(true);
         jSliderThreshold.setEnabled(true);
        pas.setText("");
        ok .setText("CLOSE");
        }
        }else{
        
       jSliderSensibility.setEnabled(false);
         jSliderThreshold.setEnabled(false);
         ok .setText("OK");
        }
    }//GEN-LAST:event_okActionPerformed

    private void sendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendActionPerformed
        // TODO add your handling code here:
       
          if(begin)
    {
      System.out.println("You clicked the stop button!");
    begin = false;
    try{ Thread.sleep(1000); } catch(Exception ex){}
    video.release();
    
    }
        String remote = "http://ltoemission.designproject.online/public/images/";
        String dir1 = remote+remoteName;
        String dir2 = remote+remoteName1;
        String success="CO:"+coresult.getText()+" HC:"+hcresult.getText();
                                  try {
            DBconnection.init();
            Connection Myconnection = DBconnection.getConnection();
            PreparedStatement ps;
            ResultSet rs;
            ps = Myconnection.prepareStatement("Insert into Results(Chassis, co, hc, first_pic, second_pic, success)values('"+this.information.getText()+"','"+this.ppm.getText()+"','"+ppm1.getText()+"','"+dir1+"','"+dir2+"','"+success+"')");
            ps.execute();
            ps = Myconnection.prepareStatement("truncate desipcdf_ltoemission.localjasper;");
            ps.execute();
            FTPUploadDirectoryTest upload = new FTPUploadDirectoryTest();
            upload.a();
            disconnect();
            this.dispose();
            delDir();
            index a = new index();
            a.show();
            
        } catch (SQLException ex) {
            Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
         JOptionPane.showMessageDialog(this, "Your data has been sent!", "Success", JOptionPane.INFORMATION_MESSAGE);
        
    }//GEN-LAST:event_sendActionPerformed

    private void send1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_send1ActionPerformed
        // TODO add your handling code here:
               
        try {
             Class.forName("com.mysql.jdbc.Driver");
              Connection Myconnection=DriverManager.getConnection("jdbc:mysql://localhost:3307/","desipcdf", "HESpcOSUKbY0");
              String reportPath="C:\\Users\\genwockz\\Documents\\NetBeansProjects\\EmissionTesting\\src\\emission\\testing\\print.jrxml";            
            JasperReport jr = JasperCompileManager.compileReport(reportPath);
             JasperPrint jp= JasperFillManager.fillReport(jr, null,Myconnection);
              JasperViewer.viewReport(jp);
               Myconnection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
        
        
    }//GEN-LAST:event_send1ActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // TODO add your handling code here:
    
        
    }//GEN-LAST:event_formWindowClosed

    private void jLabelSource1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabelSource1MouseClicked
        // TODO add your handling code here:
        
        getValues();
    }//GEN-LAST:event_jLabelSource1MouseClicked

    private void jLabelSource1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jLabelSource1KeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jLabelSource1KeyPressed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
          System.out.println("close");
         if(activate==1 || flag==1){
        disconnect();
         }
          
        stop();
        delDir();
        
         try {
            DBconnection.init();
            Connection Myconnection = DBconnection.getConnection();
            PreparedStatement ps;
            ResultSet rs;
            
            ps = Myconnection.prepareStatement("truncate desipcdf_ltoemission.localjasper;");
            ps.execute();

        } catch (SQLException ex) {
            Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
       
        this.dispose();
        index p = new index();
        p.show();
    }//GEN-LAST:event_formWindowClosing

    private void jButtonStart1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStart1ActionPerformed
        // TODO add your handling code here:
         if(begin)
    {
      System.out.println("You clicked the stop button!");
    begin = false;
    try{ Thread.sleep(1000); } catch(Exception ex){}
    video.release();
    
    }
        jButtonStart.setEnabled(true);
    }//GEN-LAST:event_jButtonStart1ActionPerformed
public void delDir(){
      File fin = new File("C:\\Users\\genwockz\\Documents\\NetBeansProjects\\EmissionTesting\\detections");
                     try {
                         FileUtils.cleanDirectory(fin);
                         System.out.println("deleted");
                     } catch (IOException ex) {
                         Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
                     }

     
     
     } 
    public void disconnect(){
    
                                         chosenPort.closePort();
					portList.setEnabled(true);
					connectButton.setEnabled(true);
                                        //jButton1.setEnabled(false);
					//series.clear();
					x = 0;
                                        y= 0;
    }
    
     public void disconnectThread(){
    
                                         chosenPort.closePort();
					portList.setEnabled(true);
					connectButton.setEnabled(true);
                                       // jButton1.setEnabled(false);
					//series.clear();
					x = 0;
                                        y=0;
                                        this.dispose();
    }
    
    private void create_graph(){
               
		JFreeChart chart = ChartFactory.createXYLineChart("Carbon Monoxide Readings", "Time (seconds)", "Raw Data Reading", dataset);
                panel.add(new ChartPanel(chart), BorderLayout.CENTER);
               
                JFreeChart chart1 = ChartFactory.createXYLineChart("HydroCarbon", "Time (seconds)", "Raw Data Reading Reading", dataset1);
                panel1.add(new ChartPanel(chart1), BorderLayout.CENTER);
    }
    
    private void populate_combo(){

        for(int i = 0; i < portNames.length; i++)
			portList.addItem(portNames[i].getSystemPortName());
    
    }
    
    
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Graph.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Graph.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Graph.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Graph.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                
               Graph mainFrame = new Graph();
              new Graph().setVisible(true);
                mainFrame.setLocationRelativeTo(null);
            }
        });
         
        
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Capture;
    public javax.swing.JLabel classif;
    public javax.swing.JButton connectButton;
    private javax.swing.JLabel coresult;
    private javax.swing.JLabel hcresult;
    public javax.swing.JLabel information;
    private javax.swing.JButton jButtonStart;
    private javax.swing.JButton jButtonStart1;
    private javax.swing.JCheckBox jCheckBoxAlarm;
    private javax.swing.JCheckBox jCheckBoxMotionDetection;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabelSource1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanelSource1;
    private javax.swing.JSlider jSliderSensibility;
    private javax.swing.JSlider jSliderThreshold;
    private javax.swing.JTextField jTextFieldSource1;
    private javax.swing.JButton ok;
    private javax.swing.JPanel panel;
    private javax.swing.JPanel panel1;
    private javax.swing.JPasswordField pas;
    private javax.swing.JLabel passingCO;
    private javax.swing.JLabel passingHC;
    private javax.swing.JComboBox<String> portList;
    private javax.swing.JLabel ppm;
    private javax.swing.JLabel ppm1;
    private javax.swing.JLabel raw;
    private javax.swing.JLabel raw1;
    private javax.swing.JLabel refresh;
    public javax.swing.JButton send;
    public javax.swing.JButton send1;
    private javax.swing.JLabel status;
    public javax.swing.JLabel year;
    // End of variables declaration//GEN-END:variables
}
