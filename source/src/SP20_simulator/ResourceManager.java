package SP20_simulator;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


/**
 * ResourceManager는 컴퓨터의 가상 리소스들을 선언하고 관리하는 클래스이다.
 * 크게 네가지의 가상 자원 공간을 선언하고, 이를 관리할 수 있는 함수들을 제공한다.
 *
 * 1) 입출력을 위한 외부 장치 또는 device
 * 2) 프로그램 로드 및 실행을 위한 메모리 공간. 여기서는 64KB를 최대값으로 잡는다.
 * 3) 연산을 수행하는데 사용하는 레지스터 공간.
 * 4) SYMTAB 등 simulator의 실행 과정에서 사용되는 데이터들을 위한 변수들.
 *
 * 2번은 simulator위에서 실행되는 프로그램을 위한 메모리공간인 반면,
 * 4번은 simulator의 실행을 위한 메모리 공간이라는 점에서 차이가 있다.
 */
public class ResourceManager {
    /**
     * 디바이스는 원래 입출력 장치들을 의미 하지만 여기서는 파일로 디바이스를 대체한다.
     * 즉, 'F1'이라는 디바이스는 'F1'이라는 이름의 파일을 의미한다.
     * deviceManager는 디바이스의 이름을 입력받았을 때 해당 이름의 파일 입출력 관리 클래스를 리턴하는 역할을 한다.
     * 예를 들어, 'A1'이라는 디바이스에서 파일을 read모드로 열었을 경우, hashMap에 <"A1", scanner(A1)> 등을 넣음으로서 이를 관리할 수 있다.
     *
     * 변형된 형태로 사용하는 것 역시 허용한다.
     * 예를 들면 key값으로 String대신 Integer를 사용할 수 있다.
     * 파일 입출력을 위해 사용하는 stream 역시 자유로이 선택, 구현한다.
     *
     * 이것도 복잡하면 알아서 구현해서 사용해도 괜찮습니다.
     */
    //device 이름과 FileReader/FileWriter를 저장하는 HashMap
    HashMap<String, Object> deviceManager = new HashMap<>();
    //메모리, 한 char에 4 bit(0.5 byte)씩 저장, 131072 * 4 = 0x80000, 524288 bit = 65536 byte = 64 Kbyte
    char[] memory = new char[131072];
    //레지스터 정보
    int[] register = new int[10];
    double register_F;
    //읽기, 출력 디바이스를 관리하는 ArrayList
    ArrayList<String> readerList = new ArrayList<>();
    ArrayList<String> writerList = new ArrayList<>();
    //SYMTAB
    SymbolTable symtab;

    /**
     * 메모리, 레지스터등 가상 리소스들을 초기화한다.
     */
    public void initializeResource() throws IOException {
        //메모리를 전부 0으로 초기화
        Arrays.fill(memory, '0');
        //레지스터를 전부 0으로 초기화
        Arrays.fill(register, 0);
        register_F = 0;
        //SYMTAB 초기화
        symtab = new SymbolTable();
        //디바이스 연결상태 초기화
        closeDevice();
    }

    /**
     * deviceManager가 관리하고 있는 파일 입출력 stream들을 전부 종료시키는 역할.
     * 프로그램을 종료하거나 연결을 끊을 때 호출한다.
     */
    public void closeDevice() throws IOException {
        //읽기 device 종료
        for (String readDiv : readerList) {
            FileReader reader = (FileReader) deviceManager.get(readDiv);
            reader.close();
        }
        //출력 device 종료
        for (String writeDiv : writerList) {
            FileWriter writer = (FileWriter) deviceManager.get(writeDiv);
            writer.close();
        }
        //읽기, 출력 리스트와 deviceManager 초기화
        readerList.clear();
        readerList = new ArrayList<>();
        writerList.clear();
        writerList = new ArrayList<>();
        deviceManager.clear();
        deviceManager = new HashMap<>();
    }

    /**
     * 디바이스를 사용할 수 있는 상황인지 체크. TD명령어를 사용했을 때 호출되는 함수.
     * 입출력 stream을 열고 deviceManager를 통해 관리시킨다.
     *
     * @param devName 확인하고자 하는 디바이스의 번호,또는 이름
     * @return 해당 디바이스가 준비가 되었으면 true, 아니면 false
     */
    public boolean testDevice(String devName) {
        //파일을 열고 읽고 쓸 수 있는 상태인지 확인
        File device = new File("./device/" + devName + ".device");
        return device.canWrite() && device.canRead();
    }

    /**
     * 디바이스로부터 원하는 개수만큼의 글자를 읽어들인다. RD명령어를 사용했을 때 호출되는 함수.
     *
     * @param devName 디바이스의 이름
     * @param num     가져오는 글자의 개수
     * @return 가져온 데이터
     */
    public char[] readDevice(String devName, int num) {
        try {
            //처음 접근하는 읽기 device라면 FileReader 추가
            if (deviceManager.get(devName) == null) {
                File rDevice = new File("./device/" + devName + ".device");
                FileReader fr = new FileReader(rDevice);
                deviceManager.put(devName, fr);
                //읽기 device 리스트에 추가
                readerList.add(devName);
            }
            //해당 device의 FileReader 불러오기
            FileReader reader = (FileReader) deviceManager.get(devName);
            int readTemp;
            char[] readInfo = new char[num * 2];
            //데이터가 있으면
            if ((readTemp = reader.read()) != -1) {
                //데이터 리턴
                readInfo[0] = (char) ((readTemp & 0xF0) >> 4);
                readInfo[1] = (char) (readTemp & 0x0F);
            }
            //데이터가 없으면
            else {
                //'\0' 리턴
                readInfo[0] = (char) (('\0' & 0xF0) >> 4);
                readInfo[1] = (char) ('\0' & 0x0F);
            }
            return readInfo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new char[]{('\0' & 0xF0) >> 4, ('\0' & 0x0F)};
    }

    /**
     * 디바이스로 원하는 개수 만큼의 글자를 출력한다. WD명령어를 사용했을 때 호출되는 함수.
     *
     * @param devName 디바이스의 이름
     * @param data    보내는 데이터
     * @param num     보내는 글자의 개수
     */
    public void writeDevice(String devName, char[] data, int num) {
        try {
            //처음 접근하는 출력 device라면 FileWriter 추가
            if (deviceManager.get(devName) == null) {
                File wDevice = new File("./device/" + devName + ".device");
                FileWriter fw = new FileWriter(wDevice);
                deviceManager.put(devName, fw);
                //출력 device 리스트에 추가
                writerList.add(devName);
            }

            //해당 device의 FileWriter 불러오기
            FileWriter writer = (FileWriter) deviceManager.get(devName);
            //데이터 출력
            String writeInfo = String.copyValueOf(data, 6 - num, num);
            writer.write(String.format("%c", Integer.parseInt(writeInfo, 16)));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 메모리의 특정 위치에서 원하는 개수만큼의 글자를 가져온다.
     *
     * @param location 메모리 접근 위치 인덱스
     * @param num      데이터 개수
     * @return 가져오는 데이터
     */
    public char[] getMemory(int location, int num) {
        return String.copyValueOf(memory, location, num).toCharArray();
    }

    /**
     * 메모리의 특정 위치에 원하는 개수만큼의 데이터를 저장한다.
     *
     * @param locate 접근 위치 인덱스
     * @param data   저장하려는 데이터
     * @param num    저장하는 데이터의 개수
     */
    public void setMemory(int locate, char[] data, int num) {
        for (int i = 0; i < num; i++)
            memory[locate + i] = data[i];
    }

    /**
     * 번호에 해당하는 레지스터가 현재 들고 있는 값을 리턴한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
     *
     * @param regNum 레지스터 분류번호
     * @return 레지스터가 소지한 값
     */
    public int getRegister(int regNum) {
        return register[regNum];
    }

    /**
     * 번호에 해당하는 레지스터에 새로운 값을 입력한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
     *
     * @param regNum 레지스터의 분류번호
     * @param value 레지스터에 집어넣는 값
     */
    public void setRegister(int regNum, int value) {
        register[regNum] = value;
    }

    /**
     * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. int값을 char[]형태로 변경한다.
     *
     * @param data char[]로 변환할 int 데이터
     * @return char[]로 변환된 데이터
     */
    public char[] intToChar(int data) {
        return String.format("%06X", data).toCharArray();
    }

    /**
     * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. char[]값을 int형태로 변경한다.
     *
     * @param data int로 변환할 char[]데이터
     * @return int로 변환된 데이터
     */
    public int byteToInt(char[] data) {
        return Integer.parseInt(String.copyValueOf(data), 16);
    }
}