package com.xstar.notebook.data.git

import com.xstar.notebook.domain.model.HostType
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

/**
 * 将 PAT 作为「密码」注入 JGit 传输。不同平台要求的 login 不同。
 */
object CredentialFactory {

    fun create(hostType: HostType, username: String, token: String): UsernamePasswordCredentialsProvider {
        val login = when (hostType) {
            HostType.GITHUB -> HostType.GITHUB.login
            HostType.GITLAB -> HostType.GITLAB.login
            HostType.GITEE -> username.ifBlank { HostType.GITEE.login }
        }
        return UsernamePasswordCredentialsProvider(login, token)
    }
}