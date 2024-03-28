package com.ownd_project.tw2023_wallet_android.oid

enum class SIOPErrors(val message: String) {
    RESPONSE_STATUS_UNEXPECTED("Received unexpected response status"),
    BAD_PARAMS("Wrong parameters provided."),
    REG_PASS_BY_REFERENCE_INCORRECTLY("Request error")
}
