package com.example.seata.at.account.service;

public interface UserService {

    /**
     * Firebase UID を基準にユーザーを同期し、内部ユーザーIDを返却する。
     * 既存ユーザーがいれば更新、存在しなければ作成する。
     */
    Long syncUser(String firebaseUid, String email, String name, String providerId);
}


