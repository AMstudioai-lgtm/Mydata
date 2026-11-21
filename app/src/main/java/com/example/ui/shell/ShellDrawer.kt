package com.example.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.entity.JournalEntity
import com.example.ui.theme.AppColors

@Composable
fun ShellDrawerContent(
    journals: List<JournalEntity>,
    selectedJournal: JournalEntity?,
    currentPageIndex: Int,
    onSelectJournal: (String) -> Unit,
    onCreateJournal: () -> Unit,
    onRequestDeleteJournal: (JournalEntity) -> Unit,
    onNavigateToPage: (Int) -> Unit
) {
    var journalSectionExpanded by remember { mutableStateOf(true) }

    ModalDrawerSheet(
        drawerContainerColor = AppColors.Panel,
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .testTag("navigation_drawer")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = AppColors.Background,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AppColors.Border),
                modifier = Modifier.size(36.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mydata_logo),
                    contentDescription = "Logo Mydata",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Mydata",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "Bases de données & Tableaux offline",
                    fontSize = 11.sp,
                    color = AppColors.TextSecondary
                )
            }
        }

        HorizontalDivider(color = AppColors.Border)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { journalSectionExpanded = !journalSectionExpanded }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = AppColors.Accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Databases",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (journalSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onCreateJournal,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("drawer_create_journal_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Créer une database",
                    tint = AppColors.Accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = journalSectionExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                if (journals.isEmpty()) {
                    Text(
                        text = "Aucune database existante",
                        fontSize = 12.sp,
                        color = AppColors.TextMuted,
                        modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 8.dp)
                    )
                } else {
                    journals.forEach { journal ->
                        val isSelected = selectedJournal?.id == journal.id && currentPageIndex == 0
                        DrawerJournalItem(
                            journal = journal,
                            isSelected = isSelected,
                            onSelect = { onSelectJournal(journal.id) },
                            onDelete = { onRequestDeleteJournal(journal) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = AppColors.Border)
        Spacer(modifier = Modifier.height(8.dp))

        DrawerNavigationItem(
            label = "Corbeille",
            icon = Icons.Default.Delete,
            selected = currentPageIndex == 1,
            testTag = "drawer_trash_button",
            onClick = { onNavigateToPage(1) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        DrawerNavigationItem(
            label = "Paramètres",
            icon = Icons.Default.Settings,
            selected = currentPageIndex == 2,
            testTag = "drawer_settings_button",
            onClick = { onNavigateToPage(2) }
        )
    }
}

@Composable
private fun DrawerJournalItem(
    journal: JournalEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(36.dp)
                .background(if (isSelected) AppColors.Accent else AppColors.Border)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .testTag("journal_item_${journal.id}"),
            shape = RoundedCornerShape(6.dp),
            color = if (isSelected) AppColors.SelectedBg else Color.Transparent,
            border = if (isSelected) BorderStroke(1.dp, AppColors.Accent.copy(alpha = 0.35f)) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = if (isSelected) AppColors.Accent else AppColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = journal.name,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) AppColors.TextPrimary else AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = AppColors.TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerNavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) AppColors.SelectedBg else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, AppColors.Accent.copy(alpha = 0.35f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) AppColors.Accent else AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary
            )
        }
    }
}
