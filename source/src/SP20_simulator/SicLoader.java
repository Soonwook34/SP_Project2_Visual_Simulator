package SP20_simulator;

import java.io.*;
import java.util.ArrayList;

/**
 * SicLoader는 프로그램을 해석해서 메모리에 올리는 역할을 수행한다. 이 과정에서 linker의 역할 또한 수행한다.
 *
 * SicLoader가 수행하는 일을 예를 들면 다음과 같다.
 * - program code를 메모리에 적재시키기
 * - 주어진 공간만큼 메모리에 빈 공간 할당하기
 * - 과정에서 발생하는 symbol, 프로그램 시작주소, control section 등 실행을 위한 정보 생성 및 관리
 */
public class SicLoader {
    ResourceManager rMgr;

    String programName;     //Program 이름
    int startAddress;       //Program의 메모리 시작 주소
    int totalLength;        //Program 총 길이
    int firstInstruction;   //Program 첫 명령어의 주소

    /**
     * SicLoader 생성자
     * @param resourceManager resourceManager
     */
    public SicLoader(ResourceManager resourceManager) {
        this.rMgr = resourceManager;
    }

    /**
     * object code를 읽어서 load과정을 수행한다. load한 데이터는 resourceManager가 관리하는 메모리에 올라가도록 한다.
     * load과정에서 만들어진 symbol table 등 자료구조 역시 resourceManager에 전달한다.
     *
     * @param objectCode 읽어들인 파일
     */
    public void load(File objectCode) {
        //정보 초기화
        programName = "";
        startAddress = 0;
        totalLength = 0;
        firstInstruction = 0;
        try {
            FileReader fileReader = new FileReader(objectCode);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;            //레코드를 한 줄씩 불러올 변수
            int sectionLength = 0;  //section의 길이 변수
            ArrayList<String> mRecord = new ArrayList<>();  //M 레코드를 나중에 수행하기 위한 임시 저장
            while ((line = bufferedReader.readLine()) != null) {
                //section 구분 라인이면 continue
                if (line.length() < 1)
                    continue;
                //레코드 별 load 수행
                switch (line.charAt(0)) {
                    //H 레코드
                    case 'H':
                        //section의 이름과 시작 주소 계산
                        String sectionName = line.substring(1, 7);
                        int sectionAddress = Integer.parseInt(line.substring(7, 13), 16) + totalLength;
                        //프로그램의 시작이라면 정보 업데이트
                        if (programName.isEmpty()) {
                            programName = sectionName;
                            startAddress = sectionAddress;
                        }
                        //section의 길이 계산
                        sectionLength = Integer.parseInt(line.substring(13), 16);
                        //section 이름과 시작 주소를 SYMTAB에 저장
                        rMgr.symtab.putSymbol(sectionName, sectionAddress);
                        break;
                    //D 레코드
                    case 'D':
                        //EXTDEF의 symbol과 주소를 SYMTAB에 저장
                        int symbolCnt = (line.length() - 1) / 12;
                        for (int i = 0; i < symbolCnt; i++) {
                            String symbol = line.substring(i * 12 + 1, i * 12 + 7);
                            int address = Integer.parseInt(line.substring(i * 12 + 7, i * 12 + 13), 16);
                            rMgr.symtab.putSymbol(symbol, address);
                        }
                        break;
                    //T 레코드
                    case 'T':
                        //시작 주소와 길이를 계산하고
                        int tStart = Integer.parseInt(line.substring(1, 7), 16) * 2 + totalLength * 2;
                        int tLength = Integer.parseInt(line.substring(7, 9), 16);
                        //메모리에 load
                        char[] tInfo = line.substring(9).toCharArray();
                        rMgr.setMemory(tStart, tInfo, tLength * 2);
                        break;
                    //M 레코드
                    case 'M':
                        //주소를 업데이트하여 임시 저장 후 Object Program을 전부 다 읽은 후 한꺼번에 수행
                        int mStart = Integer.parseInt(line.substring(1, 7), 16) * 2 + totalLength * 2;
                        int mLength = Integer.parseInt(line.substring(7, 9), 16);
                        if (mLength % 2 == 1)
                            mStart++;
                        String mInfo = line.substring(7);
                        //임시 저장
                        mRecord.add(String.format("%06X%7s", mStart, mInfo));
                        break;
                    //E 레코드
                    case 'E':
                        //Program 첫 명령어의 시작 주소를 갖고 있으면 저장
                        if (line.length() > 1)
                            firstInstruction = Integer.parseInt(line.substring(2), 16);
                        totalLength += sectionLength;
                        break;
                }
            }
            //M 레코드 수행
            for (String modify : mRecord) {
                int mStart = Integer.parseInt(modify.substring(0, 6), 16);
                int mLength = Integer.parseInt(modify.substring(6, 8), 16);
                char sign = modify.charAt(8);
                char[] memory = rMgr.getMemory(mStart, mLength);
                int original = Integer.parseInt(String.valueOf(memory), 16);
                int mAddr = rMgr.symtab.search(modify.substring(9)) * 2;
                if (sign == '+')
                    original = original * 2 + mAddr;
                else
                    original = original * 2 - mAddr;
                rMgr.setMemory(mStart, String.format("%0" + mLength + "X\n", original / 2).toCharArray(), mLength);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
