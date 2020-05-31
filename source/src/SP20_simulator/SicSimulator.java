package SP20_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 시뮬레이터로서의 작업을 담당한다. VisualSimulator에서 사용자의 요청을 받으면 이에 따라
 * ResourceManager에 접근하여 작업을 수행한다.
 *
 * 작성중의 유의사항 :
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 지양할 것.
 *  2) 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)
 *
 *
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class SicSimulator {
    ResourceManager rMgr;
    HashMap<Integer, Instruction> instMap = new HashMap<>();    //SIC/XE 머신의 명령어 정보를 저장
    InstLuncher instLuncher;                                    //실질적으로 명령어를 수행할 InstLuncher
    ArrayList<String> logList = new ArrayList<>();              //log 정보를 저장할 ArrayList
    String inst = "";                                           //명령어 정보를 저장할 변수
    int currAddr = 0;                                           //명령어의 현재 주소를 저장할 변수

    /**
     * SicSimulator 생성자
     * @param resourceManager resourceManager
     */
    public SicSimulator(ResourceManager resourceManager) {
        this.rMgr = resourceManager;
        instLuncher = new InstLuncher(resourceManager);
        //명령어 정보 불러오기
        openInstFile("inst.data");
    }

    /**
     * 입력받은 이름의 파일을 열고 해당 내용을 파싱하여 instMap에 저장한다.
     */
    public void openInstFile(String fileName) {
        try {
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            //한 줄씩 instruction 정보 가져와서 저장
            while ((line = bufferedReader.readLine()) != null) {
                Instruction inst = new Instruction(line);
                instMap.put(inst.opcode, inst);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 레지스터, 메모리 초기화 등 프로그램 load와 관련된 작업 수행.
     * 단, object code의 메모리 적재 및 해석은 SicLoader에서 수행하도록 한다.
     */
    public void load(File program) throws IOException {
        //메모리 초기화
        rMgr.initializeResource();
        //log 초기화
        logList.clear();
        logList = new ArrayList<>();
        //각종 변수 초기화
        inst = "";
        currAddr = 0;
        instLuncher = new InstLuncher(rMgr);
    }

    /**
     * 1개의 instruction이 수행된 모습을 보인다.
     */
    public boolean oneStep() {
        //PC 값 불러오기
        int locctr = rMgr.getRegister(8);
        currAddr = locctr;
        //명령어 정보 불러오기
        String tempOpcode = String.copyValueOf(rMgr.getMemory(locctr, 2));
        Instruction inst = instMap.get(Integer.parseInt(tempOpcode, 16) & 0xFC);
        //해당 명령어의 format과 nixbpe 정보 불러오기
        int format = inst.format;
        int nixbpe = Integer.parseInt(String.copyValueOf(
                rMgr.getMemory(locctr + 2, 1)), 16) | ((Integer.parseInt(tempOpcode, 16) & 0x3) << 4);
        if (format > 2 && (nixbpe & 0x1) == 1)
            format++;
        //displacement 불러오기
        char[] rest = rMgr.getMemory(locctr + 3, format * 2 - 3);
        //전체 명령어 저장
        this.inst = tempOpcode + String.format("%01X", nixbpe & 0xF) + String.copyValueOf(rest);
        locctr += format * 2;

        //명령어 수행
        locctr = instLuncher.launch(inst, nixbpe, Integer.parseInt(String.copyValueOf(rest), 16), locctr / 2);
        //log 추가
        addLog(inst.instruction);
        //PC 값 갱신하기
        rMgr.setRegister(8, locctr);
        //마지막 명령어라면 false, 아니면 true 리턴
        return locctr != 0;
    }

    /**
     * 남은 모든 instruction이 수행된 모습을 보인다.
     */
    public boolean allStep() {
        //한 명령어씩 계속 수행(Visual Simulator 참조)
        return oneStep();
    }

    /**
     * 각 단계를 수행할 때 마다 관련된 기록을 남기도록 한다.
     */
    public void addLog(String log) {
        //logList에 log 추가
        logList.add(log);
    }
}

/**
 * 명령어 하나하나의 구체적인 정보는 Instruction클래스에 담긴다.
 * instruction과 관련된 정보들을 저장하고 기초적인 연산을 수행한다.
 */
class Instruction {

    String instruction; //명령어 이름
    int opcode;         //명령어 Opcode
    int operandNum;     //명령어 피연산자 개수
    int format;         //명령어의 포맷

    /**
     * 클래스를 선언하면서 일반문자열을 즉시 구조에 맞게 파싱한다.
     * @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
     */
    public Instruction(String line) {
        parsing(line);
    }

    /**
     * 일반 문자열을 파싱하여 instruction 정보를 파악하고 저장한다.
     * @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
     */
    public void parsing(String line) {
        //탭(\t)으로 분리
        String[] info = line.split("\t");
        if (info.length == 4) {
            instruction = info[0];
            format = Integer.parseInt(info[1]);
            opcode = Integer.parseInt(info[2], 16);
            operandNum = Integer.parseInt(info[3]);
        }
    }
}
