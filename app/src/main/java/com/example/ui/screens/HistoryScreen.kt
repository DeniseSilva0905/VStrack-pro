package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShowHistoryEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val historyList by viewModel.allHistory.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StageGraphite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Histórico de Apresentações",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Registro detalhado de cada show executado, duração e faixas tocadas",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (historyList.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCard
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.EventNote, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nenhum show registrado ainda", color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text("Quando você iniciar e encerrar um show no Modo Palco, o relatório aparecerá aqui.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(historyList) { item ->
                HistoryItemCard(
                    item = item,
                    onDelete = {
                        scope.launch {
                            viewModel.repository.deleteHistory(item.id)
                            viewModel.showToast("Registro excluído.")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: ShowHistoryEntity,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.dateTimestamp) {
        SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()).format(Date(item.dateTimestamp))
    }
    val mins = item.totalDurationSeconds / 60
    val secs = item.totalDurationSeconds % 60

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.showName,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = PanicRed.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Data: $dateStr", color = TextSecondary, fontSize = 12.sp)
            Text(text = "Repertório: ${item.setlistName}", color = StageYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceCardBorder
                ) {
                    Text(
                        text = "Músicas tocadas: ${item.songsPlayedCount}",
                        color = StageYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceCardElevated
                ) {
                    Text(
                        text = "Duração: ${mins}m ${secs}s",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = item.notes, color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}
