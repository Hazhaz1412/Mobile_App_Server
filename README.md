To run this server use:
.\gradlew.bat bootRun
The server using MySQL.

Command to build:

-- 1. Bảng chính users
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255),             -- NULL nếu user chỉ đăng nhập Google
  status ENUM('PENDING', 'ACTIVE', 'DEACTIVATED', 'DELETED') 
    NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL 
    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

select * from verification_tokens;

-- 2. Bảng lưu token xác thực email
CREATE TABLE verification_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token CHAR(36) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
);

-- 3. Bảng lưu token phục hồi mật khẩu
CREATE TABLE password_reset_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token CHAR(36) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
);

-- 4. Bảng profile mở rộng
CREATE TABLE user_profiles (
  user_id BIGINT PRIMARY KEY,
  display_name VARCHAR(100) NOT NULL,
  profile_picture_url VARCHAR(500),
  bio TEXT,
  contact_info VARCHAR(255),
  rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  rating_count INT NOT NULL DEFAULT 0,
  FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
);

-- 5. Bảng liên kết Social Login
CREATE TABLE social_accounts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  provider ENUM('GOOGLE') NOT NULL,
  provider_user_id VARCHAR(255) NOT NULL,  -- ID do Google cấp
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE,
  UNIQUE (provider, provider_user_id)
);
