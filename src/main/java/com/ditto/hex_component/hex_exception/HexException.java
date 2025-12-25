package com.ditto.hex_component.hex_exception;

public class HexException extends RuntimeException{
    private static final long serialVersionUID = -7864604160297181941L;

    /** 错误码 */
    protected final HexExceptionEnum exceptionEnum;



    /**
     * 指定错误码构造通用异常
     * @param exceptionEnum 错误码
     */
    public HexException(final HexExceptionEnum exceptionEnum) {
        super(exceptionEnum.getDescription());
        this.exceptionEnum = exceptionEnum;
    }


    /**
     * Getter method for property <tt>exceptionEnum</tt>.
     *
     * @return property value of exceptionEnum
     */
    public HexExceptionEnum getExceptionEnum() {
        return exceptionEnum;
    }


}
