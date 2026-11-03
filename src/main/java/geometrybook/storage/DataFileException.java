package geometrybook.storage;

/** 데이터 파일이 손상됐거나 규칙(PK·UK·형식)에 맞지 않을 때 던지는 예외. */
public class DataFileException extends RuntimeException {
    public DataFileException(String message) {
        super(message);
    }
}
