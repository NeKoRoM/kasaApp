package com.nekorom.kasaapp.screens.tablet

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextInputService
import com.nekorom.kasaapp.R

/**
 * Компонент пошукового рядка для POS-терміналу (POS Barcode & Product Search Field).
 *
 * Призначений для:
 * 1. Ручного пошуку товарів за назвою чи внутрішнім артикулом (PLU).
 * 2. Прийому вхідного потоку символів від фізичних USB / Bluetooth сканерів штрихкодів.
 * 3. Керування фокусом введення без блокування інтерфейсу ([focusRequester] та [focusManager]).
 *
 * @param searchText Поточний вміст поля введення.
 * @param searchState Прапорець активності режиму пошуку.
 * @param focusManager Менеджер фокусу інтерфейсу.
 * @param focusRequester Запитувач фокусу для перехоплення символів зі сканера.
 * @param onChangeText Колбек зміни тексту (викликається при скануванні або введенні).
 * @param onChangeSearchState Колбек зміни стану активності пошуку.
 */
@Composable
fun SearchField(
    searchText: String,
    searchState: Boolean,
    focusManager: FocusManager,
    focusRequester: FocusRequester,
    onChangeText: (String) -> Unit,
    onChangeSearchState: (Boolean) -> Unit
) {
    TextField(
            value = searchText,
            onValueChange = {
                onChangeText(it)
            },
            trailingIcon = {
                IconButton(onClick = {
                    onChangeSearchState(!searchState)

                    if (!searchState)
                        focusRequester.requestFocus()
                    else
                        focusManager.clearFocus()

                })

                {
                    if (searchState)
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.Red
                        )
                    else
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Green
                        )


                }

            },
            modifier = Modifier
                .background(Color.Transparent)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.hasFocus)
                        onChangeSearchState(true)
                },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(R.color.teal_700),
                disabledContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),



        )

    }
