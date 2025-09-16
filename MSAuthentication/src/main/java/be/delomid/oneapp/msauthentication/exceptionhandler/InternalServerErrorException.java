package be.delomid.oneapp.msauthentication.exceptionhandler;

public class InternalServerErrorException extends RuntimeException {
    public InternalServerErrorException(String message) {
        super(message);
    }
}

