package com.example.demo.exception;

public class MemoNotFoundException extends RuntimeException {

    /** 使用缺失的备忘录 ID 构造异常信息。 */
    public MemoNotFoundException(Long memoId) {
        super("Memo not found: " + memoId);
    }
}
