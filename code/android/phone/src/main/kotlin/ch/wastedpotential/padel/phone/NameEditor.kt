package ch.wastedpotential.padel.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ch.wastedpotential.padel.board.BoardText
import ch.wastedpotential.padel.board.BoardWhite
import ch.wastedpotential.padel.display.BoardAlpha
import ch.wastedpotential.padel.display.BoardColors
import ch.wastedpotential.padel.display.UiTags
import ch.wastedpotential.padel.display.UiText
import ch.wastedpotential.padel.sync.TeamNames

// Phone-only dialog sizes, in dp / sp
private const val MAX_WIDTH = 420f
private const val CORNER = 16f
private const val PAD = 20f
private const val GAP = 10f
private const val TITLE_FONT = 18f
private const val LABEL_FONT = 12f
private const val FIELD_FONT = 18f
private const val BUTTON_FONT = 17f
private const val FIELD_CORNER = 8f
private const val FIELD_PAD_H = 12f
private const val FIELD_PAD_V = 10f
private const val SMALL_GAP = 4f

/**
 * Team-name editor. Fields start empty with the current name as placeholder; a blank field keeps it.
 * It works on its own copy, so incoming scoreboards don't disturb the typing.
 */
@Composable
fun NameEditor(current: TeamNames, onDone: (you: String, opp: String) -> Unit, onCancel: () -> Unit) {
    var you by rememberSaveable { mutableStateOf("") }
    var opp by rememberSaveable { mutableStateOf("") }
    val youFocus = remember { FocusRequester() }
    val oppFocus = remember { FocusRequester() }

    Dialog(onDismissRequest = onCancel) {
        // Inside the dialog: its fields live in the dialog's own window and composition
        LaunchedEffect(Unit) { youFocus.requestFocus() }
        Column(
            Modifier
                .widthIn(max = MAX_WIDTH.dp)
                .background(Color(BoardColors.BUTTON), RoundedCornerShape(CORNER.dp))
                .padding(PAD.dp),
            verticalArrangement = Arrangement.spacedBy(GAP.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(UiText.CANCEL, UiTags.NAMES_CANCEL, onCancel)
                BoardText(UiText.TEAM_NAMES, TITLE_FONT, BoardWhite)
                TextButton(UiText.DONE, UiTags.NAMES_DONE) { onDone(you, opp) }
            }
            NameField(
                UiText.YOUR_TEAM_FIELD,
                you,
                current.you,
                UiTags.FIELD_YOU,
                youFocus,
                ImeAction.Next,
                onChange = { you = it },
                onAction = { oppFocus.requestFocus() },
            )
            NameField(
                UiText.OPPONENT_FIELD,
                opp,
                current.opp,
                UiTags.FIELD_OPP,
                oppFocus,
                ImeAction.Done,
                onChange = { opp = it },
                onAction = { onDone(you, opp) },
            )
        }
    }
}

@Composable
private fun NameField(
    label: String,
    value: String,
    placeholder: String,
    tag: String,
    focus: FocusRequester,
    imeAction: ImeAction,
    onChange: (String) -> Unit,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SMALL_GAP.dp)) {
        BoardText(label.uppercase(), LABEL_FONT, BoardWhite.copy(alpha = BoardAlpha.DIM), weight = FontWeight.Medium)
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(TeamNames.MAX_LENGTH)) },
            singleLine = true,
            textStyle = TextStyle(color = BoardWhite, fontSize = FIELD_FONT.sp),
            cursorBrush = SolidColor(Color(BoardColors.YOU_ACCENT)),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = imeAction),
            keyboardActions = KeyboardActions { onAction() },
            modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag(tag),
            decorationBox = { field ->
                Box(
                    Modifier.background(
                        Color(BoardColors.UNDO),
                        RoundedCornerShape(FIELD_CORNER.dp),
                    ).padding(horizontal = FIELD_PAD_H.dp, vertical = FIELD_PAD_V.dp),
                ) {
                    if (value.isEmpty()) {
                        BoardText(
                            placeholder,
                            FIELD_FONT,
                            BoardWhite.copy(alpha = BoardAlpha.ICON),
                            weight = FontWeight.Normal,
                        )
                    }
                    field()
                }
            },
        )
    }
}

@Composable
private fun TextButton(label: String, tag: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = SMALL_GAP.dp)
            .testTag(tag),
    ) {
        BoardText(label, BUTTON_FONT, Color(BoardColors.YOU_ACCENT))
    }
}
