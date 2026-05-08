ALTER TABLE `user`
  ADD CONSTRAINT `chk_user_password_len` CHECK (CHAR_LENGTH(`password`) >= 6);

