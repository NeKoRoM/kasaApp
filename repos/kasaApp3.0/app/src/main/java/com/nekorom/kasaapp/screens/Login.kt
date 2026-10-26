package com.nekorom.kasaapp.screens

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.nekorom.kasaapp.MainViewModel
import com.nekorom.kasaapp.data.roomDB.entity.UserEntity
import com.nekorom.kasaapp.navigation.NavRouteLoginDrawer
import com.nekorom.kasaapp.utils.LOGIN
import com.nekorom.kasaapp.utils.PASSWORD
import com.nekorom.kasaapp.utils.USER
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.nekorom.kasaapp.R
import com.nekorom.kasaapp.btactivity.BtActivity
import com.nekorom.kasaapp.utils.Constants
import java.nio.charset.Charset

/**
 * Екран вибору профілю касира та авторизації (Login Screen).
 *
 * Основні функції:
 * 1. Відображення списку зареєстрованих локальних профілів користувачів ([UserEntity]),
 *    кожен з яких прив'язаний до конкретного фіскального номера РРО ([UserEntity.kasaId]) та MAC-адреси.
 * 2. Кнопка "registration": запуск [BtActivity] для пошуку, підключення та збереження нового РРО.
 * 3. Формування облікових даних для Firebase Auth:
 *    `email = "<kasaId>.<mac>@mcs.ua"`, `password = user.mac`.
 * 4. Перевірка ліцензії / стану акаунта в Realtime Database ([NetworkDataRepository.checkEnabled]).
 * 5. Ініціалізація зв'язку Bluetooth SPP з касою обраного касира.
 *
 * @param navHostController Контролер навігації Jetpack Compose.
 * @param viewModel Головна [MainViewModel] додатку.
 * @param mac Поточна збережена MAC-адреса.
 * @param actListLauncher Лаунчер для запуску зовнішньої активності [BtActivity] з очікуванням результату.
 */
@Composable
fun Login(
    navHostController: NavHostController,
    viewModel: MainViewModel,
    mac: String,
    actListLauncher: ActivityResultLauncher<Intent>
) {
    val context = LocalContext.current


    var dialogAdminStatr by rememberSaveable {
        mutableStateOf(viewModel.EnabledState.value ?: false)
    }
    if (dialogAdminStatr)
        MyDialog(
            onDismissRequest = { dialogAdminStatr = false },
            onConfirmation = { dialogAdminStatr = false },
            dialogTitle = stringResource(R.string.accaunt_not_active),
            dialogText = stringResource(R.string.adminPls)
        )

    //val dao = database.testDao()
    Column {


        Row {

            Button(onClick = {
                //viewModel.kasaSendMess(Command.HEADER_3_REQUEST)
                actListLauncher.launch(Intent(context, BtActivity::class.java))


            }) {

                Text(text = "registration")
            }




        }


        val users = viewModel.readAllUsers().observeAsState().value
        val products =
            viewModel.articleListF(USER ?: UserEntity(userId = 100)).observeAsState().value

        LazyColumn {
            users?.let {
                items(users) { user ->
                    UserItem(user = user,

                        onClick = {
                            val w1251: Charset = charset("Windows-1251")
                            val str = user.kasaId + "." + user.mac
                            USER = user
                            LOGIN = str
                                .replace("000", "")
                                .replace(":", "")
                                .replace("\t", "")
                                .lowercase()
                                .plus("@mcs.ua")
                            PASSWORD = user.mac
                            viewModel.logIn {
                                navHostController.navigate(NavRouteLoginDrawer.Drawer.route)
                                viewModel.writeUserToFirebase()
                                viewModel.networkDataRepository.checkEnabled() {
                                    dialogAdminStatr = true
                                    navHostController.popBackStack(
                                        NavRouteLoginDrawer.Login.route,
                                        false
                                    )
                                    viewModel.logOut()
                                    viewModel.EnabledState.value = true


                                }
                                USER?.mac?.let { it1 -> viewModel.btConnect(it1) }

                            }
                        },
                        onDelete = {
                            viewModel.deleteUser(user) {

                            }
                        }, onUserChange = {


                            viewModel.addUser(user.copy(name = it)) {
                                navHostController.popBackStack()
                                navHostController.navigate(NavRouteLoginDrawer.Login.route)
                            }
                            //viewModel.writeUserToFirebase()
                        })
                }
            }

        }


    }
}

/**
 * Елемент списку касирів / торгових точок (User Card).
 *
 * Відображає:
 * - ID користувача та найменування/адресу торгової точки.
 * - Кнопку розгортання додаткового меню керування.
 * - Можливість видалення профілю ([onDelete]).
 * - Можливість редагування назви через вбудований [TextField] та збереження ([onUserChange]).
 * - Натискання на картку ініціює вхід касира ([onClick]).
 *
 * @param user Сутність користувача з бази Room.
 * @param onDelete Колбек видалення касира.
 * @param onClick Колбек авторизації та входу в систему.
 * @param onUserChange Колбек збереження зміненого імені користувача.
 */
@Composable
fun UserItem(
    user: UserEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onUserChange: (String) -> Unit
) {


    var userNameField by remember {
        mutableStateOf(TextFieldValue(text = user.name))
    }
    var userNameText by remember {
        mutableStateOf(user.name)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 24.dp)
            .clickable {

                onClick()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {


        var menuState by remember { mutableStateOf(false) }
        Row(verticalAlignment = BiasAlignment.Vertical(1f)) {
            Column(modifier = Modifier.weight(0.8f)) {
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = user.userId.toString()
                )
                Text(fontSize = 24.sp, text = userNameText)

            }


            IconButton(modifier = Modifier.weight(0.2f), onClick = {
                menuState = !menuState
            }) {

                if (!menuState)
                    Icon(
                        Icons.Default.ArrowDropDown, contentDescription = "", modifier = Modifier
                            .fillMaxWidth()
                    ) else
                    Icon(
                        Icons.Default.ArrowDropUp, contentDescription = "", modifier = Modifier
                            .fillMaxWidth()
                    )

            }
        }
        if (menuState) {
            val textField = FocusRequester()
            var editState by remember { mutableStateOf(false) }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(3.dp)
                    .background(MaterialTheme.colorScheme.onBackground)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onDelete() }) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "",
                        tint = Color.Red,
                        modifier = Modifier

                    )
                }
                //Spacer(modifier = Modifier.fillMaxWidth())
                IconButton(onClick = {
                    editState = !editState

                    textField.requestFocus()
                    userNameField = TextFieldValue(
                        text = user.name,
                        selection = TextRange(user.name.length)
                    )
                }) {
                    Icon(
                        Icons.Default.Edit, contentDescription = "", modifier = Modifier

                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(3.dp)
                    .background(MaterialTheme.colorScheme.onBackground)
            )







            TextField(value = userNameField, onValueChange = {

                userNameField = it

            },
                Modifier
                    .focusRequester(textField)
                    .onFocusEvent {
                        if (it.isCaptured) {

                        }
                    })
            Button(onClick = { onUserChange(userNameField.text) }) {
                Text(text = "ЗМІНИТИ НАЗВУ")
            }

        }
    }


}





















