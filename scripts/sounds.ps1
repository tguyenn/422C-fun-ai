# Persistent MP3 playback through Windows' native media API.
param([Parameter(Mandatory=$true)][string]$Assets)
$ErrorActionPreference = 'Stop'
Add-Type @'
using System;
using System.Text;
using System.Runtime.InteropServices;
public static class SnakeAudio {
    [DllImport("winmm.dll", CharSet = CharSet.Unicode)]
    private static extern int mciSendString(string command, StringBuilder result, int length, IntPtr callback);
    public static void Send(string command) {
        int error = mciSendString(command, null, 0, IntPtr.Zero);
        if (error != 0) throw new Exception("MP3 playback error: " + error);
    }
}
'@
try {
    foreach ($name in @('eat', 'death')) {
        $path = Join-Path $Assets ($name + '.mp3')
        [SnakeAudio]::Send('open "' + $path + '" type mpegvideo alias ' + $name)
    }
    [Console]::Out.WriteLine('READY')
    [Console]::Out.Flush()
    while ($null -ne ($sound = [Console]::ReadLine())) {
        if ($sound -eq 'quit') { break }
        if ($sound -eq 'eat' -or $sound -eq 'death') {
            [SnakeAudio]::Send('stop eat')
            [SnakeAudio]::Send('stop death')
            [SnakeAudio]::Send('seek ' + $sound + ' to start')
            [SnakeAudio]::Send('play ' + $sound)
        }
    }
} finally {
    [SnakeAudio]::Send('close all')
}
