package com.swiftiecx.timeledger.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {

    // --- 配置区域 ---
    // 以 163 邮箱为例。如果是 QQ 邮箱，HOST 是 smtp.qq.com
    private const val SMTP_HOST = "smtpdm.aliyun.com"
    private const val SMTP_PORT = "465" // SSL端口
    private const val SENDER_EMAIL = "security@timeledger2025.xyz" // 替换为你的邮箱
    private const val SENDER_PASSWORD = "TimeLedger2025" // 替换为你的邮箱授权码

    suspend fun sendVerificationCode(toEmail: String, code: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put("mail.transport.protocol", "smtp")
                    put("mail.smtp.host", SMTP_HOST)
                    put("mail.smtp.port", SMTP_PORT)
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.socketFactory.fallback", "false")
                    put("mail.smtp.socketFactory.port", SMTP_PORT)
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, "拾光账本"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                    subject = "【拾光账本】安全验证码"
                    setText("您的验证码是：$code\n\n为了保障您的账户安全，请勿将验证码告知他人。验证码有效期为5分钟。")
                }

                Transport.send(message)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}