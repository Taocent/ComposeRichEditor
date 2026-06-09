package com.taocent.simple.compose.component.richtext.core.paste

import androidx.compose.ui.text.AnnotatedString
import com.taocent.simple.compose.component.richtext.core.document.DocumentModel
import com.taocent.simple.compose.component.richtext.core.document.DocumentModelMapper

data class RichDocumentFragment(
    val annotatedString: AnnotatedString,
    val documentModel: DocumentModel? = null
) {
    companion object {
        fun fromAnnotatedString(annotatedString: AnnotatedString): RichDocumentFragment {
            return RichDocumentFragment(
                annotatedString = annotatedString,
                documentModel = DocumentModelMapper.fromAnnotatedString(annotatedString)
            )
        }

        fun fromDocumentModel(documentModel: DocumentModel): RichDocumentFragment {
            return RichDocumentFragment(
                annotatedString = DocumentModelMapper.toAnnotatedString(documentModel),
                documentModel = documentModel
            )
        }
    }
}
