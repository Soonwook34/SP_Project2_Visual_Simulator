package SP20_simulator;

// instruction에 따라 동작을 수행하는 메소드를 정의하는 클래스

public class InstLuncher {
    ResourceManager rMgr;
    boolean returnValue;        //다음 명령어에 영향을 미치는 비교 연산의 결과 저장
    int originAddr;             //JSUB 명령어를 수행할 때 RSUB 명령어를 통해 돌아올 주소를 저장
    String currDevice = "";     //현재 명령어가 사용하고 있는 device의 이름
    String targetAddr = "";     //현재 명령어의 Target Address

    /**
     * InstLuncher 생성자
     * @param resourceManager resourceManager
     */
    public InstLuncher(ResourceManager resourceManager) {
        this.rMgr = resourceManager;
    }

    /**
     *
     * @param inst 해당 명령어 정보
     * @param nixbpe nixbpe 비트
     * @param displacement displacement
     * @param locctr 현재 PC 값
     * @return 계산된 PC 값
     */
    public int launch(Instruction inst, int nixbpe, int displacement, int locctr) {
        //디바이스 정보 초기화
        currDevice = "";
        targetAddr = "";
        //각 명령어 별로 수행
        switch (inst.instruction) {
            case "CLEAR":
                CLEAR(nixbpe);
                break;
            case "COMP":
                COMP(nixbpe, displacement, locctr);
                break;
            case "COMPR":
                COMPR(nixbpe, displacement);
                break;
            case "J":
                return J(nixbpe, displacement, locctr);
            case "JEQ":
                return JEQ(displacement, locctr);
            case "JLT":
                return JLT(displacement, locctr);
            case "JSUB":
                return JSUB(nixbpe, displacement, locctr);
            case "LDA":
                LDA(nixbpe, displacement, locctr);
                break;
            case "LDCH":
                LDCH(nixbpe, displacement, locctr);
                break;
            case "LDT":
                LDT(nixbpe, displacement, locctr);
                break;
            case "RD":
                RD(displacement, locctr);
                break;
            case "RSUB":
                return RSUB();
            case "STA":
                STA(displacement, locctr);
                break;
            case "STCH":
                STCH(nixbpe, displacement, locctr);
                break;
            case "STL":
                STL(displacement, locctr);
                break;
            case "STX":
                STX(nixbpe, displacement, locctr);
                break;
            case "TD":
                TD(displacement, locctr);
                break;
            case "TIXR":
                TIXR(nixbpe);
                break;
            case "WD":
                WD(displacement, locctr);
                break;
        }
        //계산된 PC값 리턴
        return locctr * 2;
    }

    /**CLEAR**/
    public void CLEAR(int nixbpe) {
        //해당 레지스터 초기화
        rMgr.setRegister(nixbpe, 0);
    }

    /**COMP**/
    public void COMP(int nixbpe, int displacement, int locctr) {
        //A 레지스터 값 불러오기
        int A = rMgr.getRegister(0);
        int compValue;
        if ((nixbpe & 0x30) == 0x10) {
            compValue = displacement;
            targetAddr = String.format("%06X", displacement);
        } else {
            compValue = rMgr.byteToInt(rMgr.getMemory((locctr + displacement) * 2, 6));
            targetAddr = String.format("%06X", locctr + displacement);
        }
        //비교 값 저장
        returnValue = (compValue == A);
    }

    /**COMPR**/
    public void COMPR(int nixbpe, int displacement) {
        //두 레지스터 값 불러오기
        int firstR = rMgr.getRegister(nixbpe & 0xF);
        int secondR = rMgr.getRegister(displacement);
        //비교 값 저장
        returnValue = (firstR == secondR);
    }

    /**J**/
    public int J(int nixbpe, int displacement, int locctr) {
        //indirect addressing
        if ((nixbpe & 0x30) == 0x20) {
            int indirectAddr = Integer.parseInt(String.copyValueOf(rMgr.getMemory((locctr + displacement) * 2, 6)), 16);
            //프로그램의 마지막이라면
            //  (현재 input 프로그램은 이전 주소(L 레지스터)의 정보가 없어 다시 첫번째 명령어로 돌아오는 loop가 생기므로
            //   처음으로 돌아오면 프로그램이 종료하는 것으로 약속)
            if (indirectAddr == 0) {
                targetAddr = String.format("%06X", 0);
                return 0;
            }
        }
        //displacement의 (-) 처리(sign bit 확장)
        if (displacement > 0x800)
            displacement |= 0xFFFFF000;
        targetAddr = String.format("%06X", locctr + displacement);
        return (locctr + displacement) * 2;
    }

    /**JEQ**/
    public int JEQ(int displacement, int locctr) {
        //displacement의 (-) 처리(sign bit 확장)
        if (displacement > 0x800)
            displacement |= 0xFFFFF000;
        //false면 (같지 않으면)
        if (!returnValue) {
            targetAddr = String.format("%06X", locctr);
            return locctr * 2;
        }
        //true면 (같으면)
        else {
            targetAddr = String.format("%06X", locctr + displacement);
            return (displacement + locctr) * 2;
        }
    }

    /**JLT**/
    public int JLT(int displacement, int locctr) {
        //displacement의 (-) 처리(sign bit 확장)
        if (displacement > 0x800)
            displacement |= 0xFFFFF000;
        //false면 (크거나 같으면)
        if (!returnValue) {
            targetAddr = String.format("%06X", locctr);
            return locctr * 2;
        }
        //true면 (작으면)
        else {
            targetAddr = String.format("%06X", locctr + displacement);
            return (locctr + displacement) * 2;
        }
    }

    /**JSUB**/
    public int JSUB(int nixbpe, int displacement, int locctr) {
        //돌아올 주소 저장
        originAddr = locctr;
        //4 byte format이면
        if ((nixbpe & 0x1) == 1) {
            targetAddr = String.format("%06X", displacement);
            return displacement * 2;
        }
        //3 byte format이면
        else {
            targetAddr = String.format("%06X", locctr + displacement);
            return (locctr + displacement) * 2;
        }
    }

    /**LDA**/
    public void LDA(int nixbpe, int displacement, int locctr) {
        int location;
        //immediate addressing
        if ((nixbpe & 0x30) == 0x10) {
            rMgr.setRegister(0, displacement);
            return;
        }
        //PC-relative가 아니면
        if ((nixbpe & 0x06) == 0)
            location = displacement * 2;
        else
            location = (displacement + locctr) * 2;
        //A 레지스터에 저장할 값을 불러와 A 레지스터에 반영
        int Avalue = rMgr.byteToInt(rMgr.getMemory(location, 6));
        rMgr.setRegister(0, Avalue);
        targetAddr = String.format("%06X", location / 2);
    }

    /**LDCH**/
    public void LDCH(int nixbpe, int displacement, int locctr) {
        int location;
        //PC-relative가 아니면
        if ((nixbpe & 0x06) == 0)
            location = displacement * 2;
        else
            location = (displacement + locctr) * 2;
        //X 레지스터 연산이 있다면
        if ((nixbpe & 0x08) == 0x08)
            location += rMgr.getRegister(1) * 2;
        //A 레지스터에 저장할 값을 1 byte만 불러와 A 레지스터에 반영
        int Avalue = rMgr.byteToInt(rMgr.getMemory(location, 2));
        rMgr.setRegister(0, Avalue);
        targetAddr = String.format("%06X", location / 2);
    }

    /**LDT**/
    public void LDT(int nixbpe, int displacement, int locctr) {
        int location;
        //PC-relative가 아니면
        if ((nixbpe & 0x06) == 0)
            location = displacement * 2;
        else
            location = (displacement + locctr) * 2;
        //T 레지스터에 저장할 값을 불러와 T 레지스터에 반영
        int Tvalue = rMgr.byteToInt(rMgr.getMemory(location, 6));
        rMgr.setRegister(5, Tvalue);
        targetAddr = String.format("%06X", location / 2);
    }

    /**RD**/
    public void RD(int displacement, int locctr) {
        //device에서 1 byte만큼 읽기
        String device = String.copyValueOf(rMgr.getMemory((locctr + displacement) * 2, 2));
        char[] read = rMgr.readDevice(device, 1);
        //읽은 정보를 A 레지스터에 저장
        rMgr.setRegister(0, read[0] << 4 | read[1]);
        currDevice = device;
        targetAddr = String.format("%06X", locctr + displacement);
    }

    /**RSUB**/
    public int RSUB() {
        //저장해 두었던 주소로 돌아가기
        targetAddr = String.format("%06X", originAddr);
        return originAddr * 2;
    }

    /**STA**/
    public void STA(int displacement, int locctr) {
        //A 레지스터 값을 불러와 저장
        int A = rMgr.getRegister(0);
        char[] LtoChar = rMgr.intToChar(A);
        rMgr.setMemory((locctr + displacement) * 2, LtoChar, 6);
        targetAddr = String.format("%06X", locctr + displacement);
    }

    /**STCH**/
    public void STCH(int nixbpe, int displacement, int locctr) {
        int location;
        //PC-relative가 아니면
        if ((nixbpe & 0x06) == 0)
            location = displacement * 2;
        else
            location = (displacement + locctr) * 2;
        //X 레지스터 연산이 있다면
        if ((nixbpe & 0x08) == 0x08)
            location += rMgr.getRegister(1) * 2;
        //A 레지스터에서 1 byte만 불러와 메모리에 저장
        char[] AtoChar = String.format("%02X", rMgr.getRegister(0)).toCharArray();
        rMgr.setMemory(location, AtoChar, 2);
        targetAddr = String.format("%06X", location / 2);
    }

    /**STL**/
    public void STL(int displacement, int locctr) {
        //L 레지스터에서 값을 불러와 저장
        int L = rMgr.getRegister(2);
        char[] LtoChar = rMgr.intToChar(L);
        rMgr.setMemory((locctr + displacement) * 2, LtoChar, 6);
        targetAddr = String.format("%06X", locctr + displacement);
    }

    /**STX**/
    public void STX(int nixbpe, int displacement, int locctr) {
        //X 레지스터에서 값을 불러와 저장
        int X = rMgr.getRegister(1);
        char[] XtoChar = rMgr.intToChar(X);
        int location;
        //PC-relative가 아니면
        if ((nixbpe & 0x06) == 0)
            location = displacement * 2;
        else
            location = (displacement + locctr) * 2;
        rMgr.setMemory(location, XtoChar, 6);
        targetAddr = String.format("%06X", location / 2);
    }

    /**TD**/
    public void TD(int displacement, int locctr) {
        //device 이름을 가져와 해당 device가 준비가 되었는지 확인
        String device = String.copyValueOf(rMgr.getMemory((locctr + displacement) * 2, 2));
        //device 준비 여부 저장
        returnValue = !rMgr.testDevice(device);
        currDevice = device;
        targetAddr = String.format("%06X", locctr + displacement);
    }

    /**TIXR**/
    public void TIXR(int nixbpe) {
        //X 레지스터와 비교할 레지스터 값 불러오기
        int compR = rMgr.getRegister(nixbpe & 0xF);
        //X 레지스터 값 1 증가
        int newX = rMgr.getRegister(1) + 1;
        rMgr.setRegister(1, newX);
        //비교 결과 저장
        returnValue = (newX < compR);
    }

    /**WD**/
    public void WD(int displacement, int locctr) {
        //디바이스 이름과 1 byte만큼 쓸 데이터를 불러와 해당 디바이스에 출력
        String device = String.copyValueOf(rMgr.getMemory((locctr + displacement) * 2, 2));
        char[] data = rMgr.intToChar(rMgr.getRegister(0));
        rMgr.writeDevice(device, data, 2);
        currDevice = device;
        targetAddr = String.format("%06X", locctr + displacement);
    }

}