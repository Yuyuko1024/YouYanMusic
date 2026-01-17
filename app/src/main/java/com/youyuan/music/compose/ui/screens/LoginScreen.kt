package com.youyuan.music.compose.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.ItemOuterTextButton
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.dialog.BasicDialog
import com.moriafly.salt.ui.dialog.DialogTitle
import com.moriafly.salt.ui.innerPadding
import com.moriafly.salt.ui.outerPadding
import com.youyuan.music.compose.R
import com.youyuan.music.compose.ui.viewmodel.ProfileViewModel

@UnstableSaltUiApi
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {

    var phoneNumber by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var showQrCodeDialog by remember { mutableStateOf(false) }

    val qrCodeImage by profileViewModel.qrCodeImage.collectAsState()
    val loginStatusCode by profileViewModel.loginStatusCode.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val error by profileViewModel.error.collectAsState()
    val notice by profileViewModel.notice.collectAsState()
    val isLoggedIn by profileViewModel.isLoggedIn.collectAsState()

    // 监听登录状态
    LaunchedEffect(loginStatusCode) {
        when (loginStatusCode) {
            ProfileViewModel.QR_STATUS_SUCCESS -> {
                // 登录成功，关闭对话框并回退导航
                showQrCodeDialog = false
                profileViewModel.clearQrCode()
                navController.popBackStack()
            }
            ProfileViewModel.QR_STATUS_EXPIRED -> {
                // 二维码过期，关闭对话框
                showQrCodeDialog = false
                profileViewModel.clearQrCode()
            }
        }
    }

    // 监听已登录状态
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            // 如果已经登录，直接回退
            navController.popBackStack()
        }
    }

    Column(modifier.fillMaxSize()) {
        RoundedColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            ItemEdit(
                text = phoneNumber,
                onChange = { number ->
                    phoneNumber = number
                },
                hint = "请输入+86手机号",
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth()) {
                ItemEdit(
                    text = code,
                    onChange = {
                        code = it
                    },
                    hint = "验证码",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                TextButton(
                    text = "发送验证码",
                    onClick = {
                        profileViewModel.sendCaptcha(phone = phoneNumber)
                    }
                )
            }
        }
        ItemOuterTextButton(
            text = "登录",
            onClick = {
                profileViewModel.loginWithCaptcha(
                    phone = phoneNumber,
                    captcha = code
                )
            }
        )
        ItemOuterTextButton(
            text = "扫码登录",
            onClick = {
                showQrCodeDialog = true
                profileViewModel.generateQrCode()
            }
        )

        // 显示提示信息
        notice?.let { msg ->
            Text(
                text = msg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                style = SaltTheme.textStyles.sub
            )
        }

        // 显示错误信息
        error?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = SaltTheme.colors.highlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    // 二维码登录对话框
    if (showQrCodeDialog) {
        QrCodeDialog(
            qrCodeImage = qrCodeImage,
            isLoading = isLoading,
            statusCode = loginStatusCode,
            onDismissRequest = {
                showQrCodeDialog = false
                profileViewModel.stopPolling()
                profileViewModel.clearQrCode()
            }
        )
    }
}

@UnstableSaltUiApi
@Composable
fun TextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(SaltTheme.dimens.item)
            .clickable(
                role = Role.Button,
                onClickLabel = text
            ) {
                onClick()
            }
            .innerPadding(vertical = false),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier
                .innerPadding(horizontal = false),
            color = SaltTheme.colors.highlight,
            fontWeight = FontWeight.Bold
        )
    }
}

@UnstableSaltUiApi
@Composable
fun QrCodeDialog(
    modifier: Modifier = Modifier,
    qrCodeImage: String?,
    isLoading: Boolean,
    statusCode: Int?,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
) {
    BasicDialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        DialogTitle("请使用网易云音乐App扫描下方二维码登录")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    // 加载中
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                }
                qrCodeImage != null -> {
                    // 显示二维码
                    val base64Image = qrCodeImage.substringAfter("base64,")
                    val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(250.dp)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }

        // 显示状态消息
        when (statusCode) {
            ProfileViewModel.QR_STATUS_WAITING -> {
                Text(
                    text = "请使用手机App扫描二维码",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = SaltTheme.textStyles.sub
                )
            }
            ProfileViewModel.QR_STATUS_CONFIRMING -> {
                Text(
                    text = "请在手机上确认登录",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = SaltTheme.colors.highlight,
                    fontWeight = FontWeight.Bold
                )
            }
            ProfileViewModel.QR_STATUS_SUCCESS -> {
                Text(
                    text = "登录成功！",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = SaltTheme.colors.highlight,
                    fontWeight = FontWeight.Bold
                )
            }
            ProfileViewModel.QR_STATUS_EXPIRED -> {
                Text(
                    text = "二维码已过期",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = SaltTheme.colors.highlight
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onDismissRequest()
            },
            text = stringResource(R.string.cancel),
            modifier = Modifier
                .fillMaxWidth()
                .outerPadding(),
            maxLines = 1
        )
    }

}