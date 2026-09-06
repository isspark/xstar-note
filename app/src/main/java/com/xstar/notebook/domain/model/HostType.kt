package com.xstar.notebook.domain.model

enum class HostType(val login: String) {
    GITHUB("x-access-token"),
    GITEE(""),
    GITLAB("oauth2");

    companion object {
        fun from(value: String): HostType = valueOf(value)
    }
}