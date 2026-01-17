package com.demo.ec.account.application;

public interface UserService {

    /**
     * Firebase UID を基準にユーザーを同期し、内部ユーザーIDを返却する。
     * 既存ユーザーがいれば更新、存在しなければ作成する。
     */
    Long syncUser(String firebaseUid, String email, String name, String providerId);

    /**
     * ユーザーIDを基準に本人情報を更新する。
     */
    void updatePersonalInformation(Long userId, String lastName, String firstName, 
                                   String lastNameKana, String firstNameKana, 
                                   String birthDate, String gender);
}


