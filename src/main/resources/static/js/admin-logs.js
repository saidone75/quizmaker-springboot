/*
 * QuizMaker - fun quizzes for curious minds
 * Copyright (C) 2026 Saidone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

(function () {
    const logContainer = document.getElementById('live-logs');
    const statusLabel = document.getElementById('log-status');
    const pathLabel = document.getElementById('log-path');
    const toggleRefreshBtn = document.getElementById('toggle-refresh-btn');
    const copyLogsBtn = document.getElementById('copy-logs-btn');
    let refreshPaused = false;
    let refreshIntervalId = null;

    const loadLogs = async () => {
        try {
            const response = await fetch('/api/teacher/logs/tail?lines=220', {
                headers: {'Accept': 'application/json'}
            });

            if (!response.ok) {
                statusLabel.textContent = 'Errore caricamento log (' + response.status + ')';
                return;
            }

            const payload = await response.json();
            pathLabel.textContent = 'Percorso log: ' + (payload.path || '-');

            if (payload.warning) {
                statusLabel.textContent = payload.warning;
            } else if (refreshPaused) {
                statusLabel.textContent = 'Refresh in pausa';
            } else {
                statusLabel.textContent = 'Aggiornato: ' + new Date().toLocaleTimeString('it-IT');
            }

            logContainer.textContent = (payload.lines || []).join('\n') || 'Nessuna riga disponibile.';
            logContainer.scrollTop = logContainer.scrollHeight;
        } catch (error) {
            statusLabel.textContent = 'Errore rete durante il caricamento dei log';
        }
    };

    const toggleRefresh = () => {
        refreshPaused = !refreshPaused;
        if (refreshPaused) {
            if (refreshIntervalId !== null) {
                clearInterval(refreshIntervalId);
                refreshIntervalId = null;
            }
            toggleRefreshBtn.textContent = '▶️ Riprendi refresh';
            statusLabel.textContent = 'Refresh in pausa';
        } else {
            toggleRefreshBtn.textContent = '⏸️ Stop refresh';
            loadLogs();
            refreshIntervalId = setInterval(loadLogs, 2000);
        }
    };

    const copyLogsToClipboard = async () => {
        try {
            await navigator.clipboard.writeText(logContainer.textContent || '');
            copyLogsBtn.textContent = '✅ Copiato!';
            setTimeout(() => {
                copyLogsBtn.textContent = '📋 Copia log';
            }, 1400);
        } catch (error) {
            statusLabel.textContent = 'Clipboard non disponibile nel browser';
        }
    };

    document.getElementById('refresh-logs-btn')?.addEventListener('click', loadLogs);
    toggleRefreshBtn?.addEventListener('click', toggleRefresh);
    copyLogsBtn?.addEventListener('click', copyLogsToClipboard);

    loadLogs();
    refreshIntervalId = setInterval(loadLogs, 2000);
})();
