package SP20_simulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * VisualSimulator는 사용자와의 상호작용을 담당한다.<br>
 * 즉, 버튼 클릭등의 이벤트를 전달하고 그에 따른 결과값을 화면에 업데이트 하는 역할을 수행한다.<br>
 * 실제적인 작업은 SicSimulator에서 수행하도록 구현한다.
 */
public class VisualSimulator extends JFrame {

    ResourceManager resourceManager = new ResourceManager();
    SicLoader sicLoader = new SicLoader(resourceManager);
    SicSimulator sicSimulator = new SicSimulator(resourceManager);
    //GUI 업데이트 변수
    public static JFrame frame;
    private JPanel mainPanel;
    private JTextField JTextFieldFileName;
    private JButton JButtonOpen;
    private JTextArea JTextAreaLog;
    private JTextField JTextFieldADec;
    private JTextField JTextFieldAHex;
    private JTextField JTextFieldXDec;
    private JTextField JTextFieldXHex;
    private JTextField JTextFieldLDec;
    private JTextField JTextFieldLHex;
    private JTextField JTextFieldPCDec;
    private JTextField JTextFieldPCHex;
    private JTextField JTextFieldBDec;
    private JTextField JTextFieldSDec;
    private JTextField JTextFieldTDec;
    private JTextField JTextFieldBHex;
    private JTextField JTextFieldSHex;
    private JTextField JTextFieldTHex;
    private JTextField JTextFieldSWHex;
    private JTextField JTextFieldFHex;
    private JTextField JTextFieldFirstInst;
    private JTextField JTextFieldInstStartAddr;
    private JTextField JTextFieldDevice;
    private JButton JButton1Step;
    private JButton JButtonAll;
    private JButton JButtonExit;
    private JTextField JTextFieldTargetAddr;
    private JTextField JTextFieldProgramName;
    private JTextField JTextFieldStartAddr;
    private JTextField JTextFieldProgramLength;
    private JPanel JPanelInstruction;
    private JList JListInstruction;

    /**
     * 메인 함수
     */
    public static void main(String[] args) {
        frame = new VisualSimulator("SIC/XE Simulator");    //프로그램 실행
    }

    /**
     * VisualSimulator 생성자
     * GUI 창을 생성하고 띄운다.
     */
    public VisualSimulator(String title) {
        super(title);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        this.pack();
        this.setResizable(false);

        //GUI form에서 생성하지 못한 명령어 기록 생성
        JListInstruction = new JList(new DefaultListModel());
        JListInstruction.setSize(100, 250);
        JListInstruction.setVisibleRowCount(8);
        JListInstruction.setAutoscrolls(true);
        JPanelInstruction.add(new JScrollPane(JListInstruction));

        //각 버튼에 ActionListener 추가
        JButtonOpen.addActionListener(new JButtonOpenActionListener());
        JButton1Step.addActionListener(new JButton1StepActionListener());
        JButtonAll.addActionListener(new JButtonAllActionListener());
        JButtonExit.addActionListener(new JButtonExitActionListener());

        this.setVisible(true);
    }

    /**
     * Open 버튼의 ActionListener
     * Object Program 파일을 열고 load
     */
    private class JButtonOpenActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            //Object Program 파일 가져오기
            FileDialog fileDialog = new FileDialog(VisualSimulator.frame, "Open File", FileDialog.LOAD);
            fileDialog.setFile("*.obj");
            fileDialog.setDirectory("../../object code");
            fileDialog.setVisible(true);
            String fileName = fileDialog.getFile();
            String path = fileDialog.getDirectory() + fileName;

            //Object Program Load 하기
            try {
                load(new File(path));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * 1Step 버튼의 ActionListener
     * Object Program의 명령을 1개 실행
     */
    private class JButton1StepActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                oneStep();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * ALl 버튼의 ActionListener
     * Object Program의 명렁을 종료 시까지 실행
     */
    private class JButtonAllActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                allStep();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Exit 버튼의 ActionListener
     * 프로그램을 종료
     */
    private class JButtonExitActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                resourceManager.closeDevice();
                System.exit(0);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * 프로그램 로드 명령을 전달한다.
     */
    public void load(File program) throws IOException {
        //파일이 있으면
        if (program.exists()) {
            //load 실행
            JTextFieldFileName.setText(program.getName());
            sicSimulator.load(program);
            sicLoader.load(program);
            initInfo();
        }
        // 파일이 없으면
        else
            JOptionPane.showMessageDialog(null, "[지정된 파일을 찾을 수 없습니다.]\n" + program.getPath(),
                    "파일 없음", JOptionPane.WARNING_MESSAGE);
    }


    /**
     * 하나의 명령어만 수행할 것을 SicSimulator에 요청한다.
     */
    public void oneStep() throws IOException {
        if (sicSimulator.oneStep() == false) {
            //프로그램이 종료되면 버튼을 비활성화하고 device들을 전부 닫는다
            JButton1Step.setEnabled(false);
            JButtonAll.setEnabled(false);
            resourceManager.closeDevice();
        }
        //GUI 업데이트
        update();
    }

    /**
     * 남아있는 모든 명령어를 수행할 것을 SicSimulator에 요청한다.
     */
    public void allStep() throws IOException {
        while (sicSimulator.allStep()) {
            update();
        }
        //끝까지 실행한 후에 버튼을 비활성화하고 device들을 전부 닫는다
        JButton1Step.setEnabled(false);
        JButtonAll.setEnabled(false);
        resourceManager.closeDevice();
        //GUI 업데이트
        update();
    }

    /**
     * 화면을 최신값으로 갱신하는 역할을 수행한다.
     */
    public void update() {
        //각 레지스터 값 가져오기
        int A = resourceManager.getRegister(0);
        int X = resourceManager.getRegister(1);
        int L = resourceManager.getRegister(2);
        int B = resourceManager.getRegister(3);
        int S = resourceManager.getRegister(4);
        int T = resourceManager.getRegister(5);
        int F = 0;
        int PC = resourceManager.getRegister(8);
        int SW = 0;

        //A 레지스터 업데이트
        JTextFieldADec.setText(String.format("%d", A));
        JTextFieldAHex.setText(String.format("%06X", A));
        //X 레지스터 업데이트
        JTextFieldXDec.setText(String.format("%d", X));
        JTextFieldXHex.setText(String.format("%06X", X));
        //L 레지스터 업데이트
        JTextFieldLDec.setText(String.format("%d", L));
        JTextFieldLHex.setText(String.format("%06X", L));
        //PC 레지스터 업데이트
        JTextFieldPCDec.setText(String.format("%d", PC / 2));
        JTextFieldPCHex.setText(String.format("%06X", PC / 2));
        //SW 레지스터 업데이트
        JTextFieldSWHex.setText(String.format("%06X", SW));
        //B 레지스터 업데이트
        JTextFieldBDec.setText(String.format("%d", B));
        JTextFieldBHex.setText(String.format("%06X", B));
        //S 레지스터 업데이트
        JTextFieldSDec.setText(String.format("%d", S));
        JTextFieldSHex.setText(String.format("%06X", S));
        //T 레지스터 업데이트
        JTextFieldTDec.setText(String.format("%d", T));
        JTextFieldTHex.setText(String.format("%06X", T));
        //F 레지스터 업데이트
        JTextFieldFHex.setText(String.format("%06X", F));

        //명령어의 시작 주소 업데이트
        JTextFieldInstStartAddr.setText(String.format("%06X", sicSimulator.currAddr / 2));
        //명령어의 Target Address 업데이트
        JTextFieldTargetAddr.setText(sicSimulator.instLuncher.targetAddr);
        //현재 사용중인 device 업데이트
        JTextFieldDevice.setText(sicSimulator.instLuncher.currDevice);

        //log 업데이트
        if (sicSimulator.logList.size() > 0) {
            String log = sicSimulator.logList.get(sicSimulator.logList.size() - 1);
            if (JTextAreaLog.getDocument().getLength() == 0)
                JTextAreaLog.append(log);
            else
                JTextAreaLog.append('\n' + log);
            JTextAreaLog.setCaretPosition(JTextAreaLog.getDocument().getLength());
        }
        //명령어 업데이트
        if (sicSimulator.inst.length() > 0) {
            DefaultListModel model = (DefaultListModel) JListInstruction.getModel();
            model.addElement(sicSimulator.inst);
            int size = model.size();
            JListInstruction.setSelectedIndex(size - 1);
        }
    }

    /**
     * Object Program이 처음 load될 때 고정 값 업데이트
     */
    public void initInfo() {
        //Program 이름 업데이트
        JTextFieldProgramName.setText(sicLoader.programName);
        //Program 메모리 시작 주소 업데이트
        JTextFieldStartAddr.setText(String.format("%06X", sicLoader.startAddress));
        //Program 총 길이 업데이트
        JTextFieldProgramLength.setText(String.format("%06X", sicLoader.totalLength));
        //Program 첫 명령어 주소 업데이트
        JTextFieldFirstInst.setText(String.format("%06X", sicLoader.firstInstruction));
        update();

        //1step, all 버튼 활성화
        JButton1Step.setEnabled(true);
        JButtonAll.setEnabled(true);

        //로그, 명령어 초기화
        JTextAreaLog.setText("");
        DefaultListModel model = (DefaultListModel) JListInstruction.getModel();
        model.clear();
    }
}
