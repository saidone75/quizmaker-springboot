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
