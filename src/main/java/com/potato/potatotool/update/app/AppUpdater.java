package com.potato.potatotool.update.app;

import com.potato.potatotool.storage.PathManager;
import com.potato.potatotool.update.UpdateChecker;
import com.potato.potatotool.update.downloader.DownloadProgressCallback;
import com.potato.potatotool.update.downloader.MultiSourceDownloader;
import com.potato.potatotool.update.manifest.Manifest;
import com.potato.potatotool.update.verifier.FileVerifier;
import com.potato.potatotool.utils.core.I18nUtils;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.potato.potatotool.ToStart.debugMode;

/**
 * 软件更新器 - 使用外部脚本方案
 * 
 * @author Potato
 * @date 2025/10/11
 */
public class AppUpdater {
    
    private final MultiSourceDownloader downloader;
    private final PathManager pathManager;
    
    public AppUpdater() {
        this.downloader = new MultiSourceDownloader();
        this.pathManager = PathManager.getInstance();
    }
    
    /**
     * 下载并准备软件更新
     * @param appVersion 新版本信息
     * @param callback 下载进度回调
     * @throws Exception 下载或准备失败
     */
    public void downloadAndPrepareUpdate(Manifest.AppVersion appVersion, 
                                        DownloadProgressCallback callback) throws Exception {
        // 1. 获取平台标识
        String platform = UpdateChecker.getPlatformIdentifier();
        
        // 2. 获取对应平台的文件信息
        Manifest.FileInfo fileInfo = appVersion.getFiles().get(platform);
        if (fileInfo == null) {
            throw new Exception(I18nUtils.getString("update.exception.platform.not.found", platform));
        }
        
        // 3. 获取下载URL并选择最佳源
        List<String> urls = fileInfo.getUrl().getAllUrls();
        if (urls.isEmpty()) {
            throw new Exception(I18nUtils.getString("update.exception.no.url"));
        }
        
        // 智能选择最佳下载源（静默选择，外层会显示状态）
        if (urls.size() > 1) {
            if (debugMode) System.out.println("检测到多个下载源，正在选择最佳源...");
            String bestUrl = downloader.selectBestSource(urls);
            if (bestUrl != null) {
                if (debugMode) System.out.println("已选择最佳下载源");
                urls.remove(bestUrl);
                urls.add(0, bestUrl);
            }
        }
        
        // 4. 确定下载目标路径
        Path updateDir = pathManager.getTempPath().resolve("update");
        Files.createDirectories(updateDir);
        
        String jarName = extractJarName(urls.get(0));
        Path newJar = updateDir.resolve(jarName);
        
        // 5. 检查是否已下载并校验
        if (Files.exists(newJar)) {
            try {
                long fileSize = Files.size(newJar);
                if (debugMode) {
                    System.out.println("检测到已下载的文件: " + newJar + " (" + PathManager.formatSize(fileSize) + ")");
                    System.out.println("校验文件...");
                }
                
                if (FileVerifier.verifyChecksum(newJar, fileInfo.getChecksum())) {
                    if (debugMode) System.out.println("文件校验通过，跳过下载");
                    
                    // 通知回调（使用缓存）
                    if (callback != null) {
                        callback.onStart(urls.get(0), fileSize);
                        callback.onProgress(fileSize, fileSize, 100, 0);
                        callback.onComplete(newJar);
                    }
                    
                    // 生成更新脚本
                    generateUpdateScript(newJar);
                    if (debugMode) System.out.println("软件更新已准备就绪（使用缓存），可以重启安装");
                    return;
                } else {
                    if (debugMode) System.out.println("文件校验失败，删除并重新下载");
                    Files.deleteIfExists(newJar);
                }
            } catch (Exception e) {
                System.err.println("检查已下载文件时出错: " + e.getMessage());
                try {
                    Files.deleteIfExists(newJar);
                } catch (Exception ex) {
                    // 忽略
                }
            }
        }
        
        if (debugMode) System.out.println("开始下载软件更新...");
        
        // 6. 下载新版本
        downloader.downloadWithFallback(urls, newJar, callback);
        
        // 7. 校验文件完整性
        if (debugMode) System.out.println("校验下载的文件...");
        if (!FileVerifier.verifyChecksum(newJar, fileInfo.getChecksum())) {
            Files.deleteIfExists(newJar);
            throw new Exception(I18nUtils.getString("update.exception.checksum.failed"));
        }
        
        // 8. 生成更新脚本
        generateUpdateScript(newJar);
        
        if (debugMode) System.out.println("软件更新已准备就绪，可以重启安装");
    }
    
    /**
     * 生成更新脚本
     */
    private void generateUpdateScript(Path newJar) throws Exception {
        String currentJarPath = getCurrentJarPath();
        String javaPath = getJavaExecutablePath();
        Path updateDir = newJar.getParent();
        
        boolean isWindows = isWindows();
        String scriptName = isWindows ? "update.bat" : "update.sh";
        Path scriptPath = updateDir.resolve(scriptName);
        Path logPath = updateDir.resolve("update.log");
        
        String script = isWindows ? 
                generateWindowsScript(currentJarPath, newJar.toString(), javaPath, logPath.toString()) :
                generateUnixScript(currentJarPath, newJar.toString(), javaPath, logPath.toString());
        
        Files.write(scriptPath, script.getBytes(StandardCharsets.UTF_8));
        
        if (!isWindows) {
            scriptPath.toFile().setExecutable(true);
        }
        
        if (debugMode) {
            System.out.println("更新脚本已生成: " + scriptPath);
            System.out.println("使用 Java 路径: " + javaPath);
        }
    }
    
    /**
     * 生成Windows更新脚本
     */
    private String generateWindowsScript(String currentJar, String newJar, String javaPath, String logPath) {
        return String.format(
            "@echo off\n" +
            "chcp 65001 > nul\n" +
            "title PotatoTool 更新程序\n" +
            "color 0A\n" +
            "\n" +
            "echo ========================================\n" +
            "echo    PotatoTool 更新程序\n" +
            "echo ========================================\n" +
            "echo.\n" +
            "echo Java 路径: %s\n" +
            "echo 日志文件: %s\n" +
            "echo.\n" +
            "\n" +
            ":: 写入日志头部\n" +
            "echo PotatoTool 更新脚本 > \"%s\"\n" +
            "echo ================================ >> \"%s\"\n" +
            "echo 当前时间: %%date%% %%time%% >> \"%s\"\n" +
            "echo Java 路径: %s >> \"%s\"\n" +
            "echo 当前 JAR: %s >> \"%s\"\n" +
            "echo 新版 JAR: %s >> \"%s\"\n" +
            "echo. >> \"%s\"\n" +
            "\n" +
            "echo [1/4] 等待程序退出...\n" +
            "echo 等待程序退出... >> \"%s\"\n" +
            "timeout /t 3 /nobreak > nul\n" +
            "echo       完成！\n" +
            "echo.\n" +
            "\n" +
            ":: 备份旧版本\n" +
            "echo [2/4] 备份旧版本...\n" +
            "echo 备份旧版本... >> \"%s\"\n" +
            "move /y \"%s\" \"%s.backup\" > nul 2>&1\n" +
            "if errorlevel 1 (\n" +
            "    echo       警告: 备份失败或旧版本不存在\n" +
            "    echo 警告: 备份失败或旧版本不存在 >> \"%s\"\n" +
            ") else (\n" +
            "    echo       完成！\n" +
            "    echo 备份成功 >> \"%s\"\n" +
            ")\n" +
            "echo.\n" +
            "\n" +
            ":: 复制新版本\n" +
            "echo [3/4] 安装新版本...\n" +
            "echo 复制新版本... >> \"%s\"\n" +
            "copy /y \"%s\" \"%s\" > nul 2>&1\n" +
            "if errorlevel 1 (\n" +
            "    echo       失败！\n" +
            "    echo.\n" +
            "    echo 错误: 更新失败，正在恢复旧版本... >> \"%s\"\n" +
            "    echo 错误: 更新失败！正在恢复旧版本...\n" +
            "    move /y \"%s.backup\" \"%s\" > nul 2>&1\n" +
            "    echo.\n" +
            "    echo 请查看日志文件: %s\n" +
            "    echo.\n" +
            "    echo 按任意键退出...\n" +
            "    pause > nul\n" +
            "    exit /b 1\n" +
            ")\n" +
            "echo       完成！\n" +
            "echo 更新成功 >> \"%s\"\n" +
            "echo.\n" +
            "\n" +
            ":: 启动新版本\n" +
            "echo [4/4] 启动应用...\n" +
            "echo 启动应用: \"%s\" -jar \"%s\" >> \"%s\"\n" +
            "start \"\" \"%s\" -jar \"%s\"\n" +
            "if errorlevel 1 (\n" +
            "    echo       失败！\n" +
            "    echo.\n" +
            "    echo 错误: 启动失败 >> \"%s\"\n" +
            "    echo 错误: 启动失败！请手动启动应用。\n" +
            "    echo.\n" +
            "    echo 请查看日志文件: %s\n" +
            "    echo.\n" +
            "    pause\n" +
            "    exit /b 1\n" +
            ")\n" +
            "echo       完成！\n" +
            "echo 应用已启动 >> \"%s\"\n" +
            "echo.\n" +
            "\n" +
            "echo ========================================\n" +
            "echo    更新成功完成！\n" +
            "echo ========================================\n" +
            "echo.\n" +
            "echo 窗口将在 3 秒后自动关闭...\n" +
            "timeout /t 3 /nobreak > nul\n" +
            "\n" +
            ":: 清理临时文件\n" +
            "echo 清理临时文件... >> \"%s\"\n" +
            "del \"%s.backup\" > nul 2>&1\n" +
            "del \"%s\" > nul 2>&1\n" +
            "echo 临时文件已清理 >> \"%s\"\n" +
            "del \"%%~f0\"\n",
            // 窗口标题和头部
            javaPath, logPath,
            // 日志头部
            logPath, logPath, logPath, javaPath, logPath, currentJar, logPath, newJar, logPath, logPath,
            // [1/4] 等待退出
            logPath,
            // [2/4] 备份
            logPath, currentJar, currentJar, logPath, logPath,
            // [3/4] 复制
            logPath, newJar, currentJar, logPath, currentJar, currentJar, logPath, logPath,
            // [4/4] 启动
            javaPath, currentJar, logPath, javaPath, currentJar, logPath, logPath, logPath,
            // 清理
            logPath, currentJar, newJar, logPath
        );
    }
    
    /**
     * 生成Unix/macOS更新脚本
     */
    private String generateUnixScript(String currentJar, String newJar, String javaPath, String logPath) {
        return String.format(
            "#!/bin/bash\n" +
            "\n" +
            "# 设置日志文件\n" +
            "LOG_FILE=\"%s\"\n" +
            "\n" +
            "# 设置颜色\n" +
            "GREEN='\\033[0;32m'\n" +
            "YELLOW='\\033[1;33m'\n" +
            "RED='\\033[0;31m'\n" +
            "NC='\\033[0m' # No Color\n" +
            "\n" +
            "# 日志和显示函数\n" +
            "log_and_show() {\n" +
            "    echo \"[$(date '+%%Y-%%m-%%d %%H:%%M:%%S')]\" \"$@\" | tee -a \"$LOG_FILE\"\n" +
            "}\n" +
            "\n" +
            "show() {\n" +
            "    echo -e \"$@\"\n" +
            "}\n" +
            "\n" +
            "show \"========================================\"\n" +
            "show \"   PotatoTool 更新程序\"\n" +
            "show \"========================================\"\n" +
            "show \"\"\n" +
            "show \"Java 路径: %s\"\n" +
            "show \"日志文件: %s\"\n" +
            "show \"\"\n" +
            "\n" +
            "# 写入日志头部\n" +
            "log_and_show \"========================================\"\n" +
            "log_and_show \"PotatoTool 更新脚本\"\n" +
            "log_and_show \"========================================\"\n" +
            "log_and_show \"Java 路径: %s\"\n" +
            "log_and_show \"当前 JAR: %s\"\n" +
            "log_and_show \"新版 JAR: %s\"\n" +
            "log_and_show \"操作系统: $OSTYPE\"\n" +
            "log_and_show \"\"\n" +
            "\n" +
            "# 等待程序退出\n" +
            "show \"[1/4] 等待程序退出...\"\n" +
            "log_and_show \"等待程序退出...\"\n" +
            "sleep 3\n" +
            "show \"      ${GREEN}完成！${NC}\"\n" +
            "show \"\"\n" +
            "\n" +
            "# 备份旧版本\n" +
            "show \"[2/4] 备份旧版本...\"\n" +
            "log_and_show \"备份旧版本...\"\n" +
            "if [ -f \"%s\" ]; then\n" +
            "    mv \"%s\" \"%s.backup\" 2>/dev/null\n" +
            "    if [ $? -eq 0 ]; then\n" +
            "        show \"      ${GREEN}完成！${NC}\"\n" +
            "        log_and_show \"备份成功\"\n" +
            "    else\n" +
            "        show \"      ${YELLOW}警告: 备份失败${NC}\"\n" +
            "        log_and_show \"警告: 备份失败\"\n" +
            "    fi\n" +
            "else\n" +
            "    show \"      ${YELLOW}警告: 旧版本文件不存在${NC}\"\n" +
            "    log_and_show \"警告: 旧版本文件不存在\"\n" +
            "fi\n" +
            "show \"\"\n" +
            "\n" +
            "# 复制新版本\n" +
            "show \"[3/4] 安装新版本...\"\n" +
            "log_and_show \"复制新版本...\"\n" +
            "cp \"%s\" \"%s\" 2>&1 | tee -a \"$LOG_FILE\" > /dev/null\n" +
            "if [ $? -ne 0 ]; then\n" +
            "    show \"      ${RED}失败！${NC}\"\n" +
            "    show \"\"\n" +
            "    log_and_show \"错误: 更新失败，正在恢复旧版本...\"\n" +
            "    show \"${RED}错误: 更新失败！正在恢复旧版本...${NC}\"\n" +
            "    if [ -f \"%s.backup\" ]; then\n" +
            "        mv \"%s.backup\" \"%s\" 2>/dev/null\n" +
            "        log_and_show \"已恢复旧版本\"\n" +
            "    fi\n" +
            "    show \"\"\n" +
            "    show \"请查看日志文件: $LOG_FILE\"\n" +
            "    show \"\"\n" +
            "    show \"按回车键退出...\"\n" +
            "    read -r\n" +
            "    exit 1\n" +
            "fi\n" +
            "show \"      ${GREEN}完成！${NC}\"\n" +
            "log_and_show \"更新成功\"\n" +
            "show \"\"\n" +
            "\n" +
            "# 启动新版本\n" +
            "show \"[4/4] 启动应用...\"\n" +
            "log_and_show \"启动应用...\"\n" +
            "if [[ \"$OSTYPE\" == \"darwin\"* ]]; then\n" +
            "    # macOS - 使用 Java 命令启动（确保能正确启动JAR）\n" +
            "    log_and_show \"macOS: 使用 Java 启动: %s -jar %s\"\n" +
            "    nohup \"%s\" -jar \"%s\" > /dev/null 2>&1 &\n" +
            "    RESULT=$?\n" +
            "    # 等待一下确保进程启动\n" +
            "    sleep 1\n" +
            "else\n" +
            "    # Linux - 使用指定的 Java 路径\n" +
            "    log_and_show \"Linux: 使用 Java 启动: %s -jar %s\"\n" +
            "    nohup \"%s\" -jar \"%s\" > /dev/null 2>&1 &\n" +
            "    RESULT=$?\n" +
            "    sleep 1\n" +
            "fi\n" +
            "\n" +
            "if [ $RESULT -eq 0 ]; then\n" +
            "    show \"      ${GREEN}完成！${NC}\"\n" +
            "    log_and_show \"应用已启动\"\n" +
            "else\n" +
            "    show \"      ${RED}失败！${NC}\"\n" +
            "    show \"\"\n" +
            "    log_and_show \"错误: 启动失败 (退出码: $RESULT)\"\n" +
            "    show \"${RED}错误: 启动失败！请手动启动应用。${NC}\"\n" +
            "    show \"\"\n" +
            "    show \"请查看日志文件: $LOG_FILE\"\n" +
            "    show \"\"\n" +
            "    read -r\n" +
            "    exit 1\n" +
            "fi\n" +
            "show \"\"\n" +
            "\n" +
            "show \"========================================\"\n" +
            "show \"   ${GREEN}更新成功完成！${NC}\"\n" +
            "show \"========================================\"\n" +
            "show \"\"\n" +
            "show \"窗口将在 3 秒后自动关闭...\"\n" +
            "sleep 3\n" +
            "\n" +
            "# 清理临时文件\n" +
            "log_and_show \"清理临时文件...\"\n" +
            "rm -f \"%s.backup\" 2>/dev/null\n" +
            "rm -f \"%s\" 2>/dev/null\n" +
            "log_and_show \"临时文件已清理\"\n" +
            "log_and_show \"更新完成\"\n" +
            "\n" +
            "# 删除脚本自身\n" +
            "rm -- \"$0\" 2>/dev/null\n",
            // 日志设置
            logPath,
            // 头部显示
            javaPath, logPath,
            // 日志头部
            javaPath, currentJar, newJar,
            // [2/4] 备份
            currentJar, currentJar, currentJar,
            // [3/4] 复制
            newJar, currentJar,
            currentJar, currentJar, currentJar,
            // [4/4] 启动 (macOS)
            javaPath, currentJar, javaPath, currentJar,
            // 启动 (Linux)
            javaPath, currentJar, javaPath, currentJar,
            // 清理
            currentJar, newJar
        );
    }
    
    /**
     * 执行更新（启动脚本并退出应用）
     */
    public void executeUpdate() throws Exception {
        Path updateDir = pathManager.getTempPath().resolve("update");
        boolean isWindows = isWindows();
        String scriptName = isWindows ? "update.bat" : "update.sh";
        Path scriptPath = updateDir.resolve(scriptName);
        
        if (!Files.exists(scriptPath)) {
            throw new Exception(I18nUtils.getString("update.exception.script.not.found", scriptPath));
        }
        
        if (debugMode) {
            System.out.println("启动更新脚本，应用即将重启...");
            System.out.println("脚本路径: " + scriptPath);
        }
        
        // 启动更新脚本
        ProcessBuilder pb;
        if (isWindows) {
            // Windows: 使用 start 命令在新窗口中启动，脚本独立于父进程
            // 注意：不使用 /wait，让脚本在新窗口中独立运行
            pb = new ProcessBuilder("cmd", "/c", "start", "PotatoTool更新", scriptPath.toString());
            if (debugMode) System.out.println("使用 start 命令在新窗口中启动更新脚本");
        } else {
            // Unix/macOS/Linux: 在终端窗口中显示更新过程
            
            if (isMacOS()) {
                // macOS: 使用 AppleScript 在新终端窗口中执行脚本
                // 这样用户可以看到输出，且脚本独立于父进程
                String escapedPath = scriptPath.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
                
                String appleScript = String.format(
                    "tell application \"Terminal\"\n" +
                    "    activate\n" +
                    "    do script \"bash '%s'\"\n" +
                    "end tell",
                    escapedPath.replace("'", "'\\''")  // 处理单引号
                );
                
                if (debugMode) System.out.println("使用 AppleScript 在新终端窗口中启动更新脚本");
                pb = new ProcessBuilder("osascript", "-e", appleScript);
                
            } else {
                // Linux: 尝试在终端窗口中显示（支持多种终端模拟器）
                // 优先级：x-terminal-emulator > gnome-terminal > konsole > xterm
                String terminal = detectLinuxTerminal();
                
                if (terminal != null) {
                    if (debugMode) System.out.println("使用 " + terminal + " 在新终端窗口中启动更新脚本");
                    
                    if (terminal.contains("gnome-terminal")) {
                        // GNOME Terminal
                        pb = new ProcessBuilder(terminal, "--", "bash", scriptPath.toString());
                    } else if (terminal.contains("konsole")) {
                        // KDE Konsole
                        pb = new ProcessBuilder(terminal, "-e", "bash", scriptPath.toString());
                    } else if (terminal.contains("xterm")) {
                        // xterm
                        pb = new ProcessBuilder(terminal, "-e", "bash", scriptPath.toString());
                    } else if (terminal.contains("x-terminal-emulator")) {
                        // Debian/Ubuntu 默认终端
                        pb = new ProcessBuilder(terminal, "-e", "bash " + scriptPath.toString());
                    } else {
                        // 未知终端，尝试通用方式
                        pb = new ProcessBuilder(terminal, "-e", "bash", scriptPath.toString());
                    }
                } else {
                    // 没有找到图形终端，使用后台方式（静默运行）
                    if (debugMode) {
                        System.out.println("警告: 未检测到图形终端，将在后台静默运行更新脚本");
                        System.out.println("日志将写入: " + updateDir.resolve("update.log"));
                    }
                    
                    // 使用 nohup 在后台运行，输出重定向到日志文件
                    Path wrapperScript = updateDir.resolve("run_update.sh");
                    String wrapperContent = String.format(
                        "#!/bin/bash\n" +
                        "# 在后台运行更新脚本，完全脱离父进程\n" +
                        "cd \"%s\"\n" +
                        "nohup bash \"%s\" >> update.log 2>&1 &\n" +
                        "exit 0\n",
                        updateDir.toString(),
                        scriptPath.toString()
                    );
                    Files.write(wrapperScript, wrapperContent.getBytes(StandardCharsets.UTF_8));
                    wrapperScript.toFile().setExecutable(true);
                    
                    pb = new ProcessBuilder("bash", wrapperScript.toString());
                }
            }
        }
        
        pb.directory(updateDir.toFile());
        
        Process process = pb.start();
        
        if (debugMode) System.out.println("更新脚本已启动 (PID: " + getPid(process) + ")");
        
        // 等待一小会儿确保脚本启动
        Thread.sleep(500);
        
    }
    
    /**
     * 判断是否是 macOS
     */
    private boolean isMacOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac") || osName.contains("darwin");
    }
    
    /**
     * 检测 Linux 系统中可用的终端模拟器
     * @return 终端命令，如果未找到返回 null
     */
    private String detectLinuxTerminal() {
        // 按优先级检测常见的终端模拟器
        String[] terminals = {
            "x-terminal-emulator",  // Debian/Ubuntu 默认
            "gnome-terminal",       // GNOME
            "konsole",              // KDE
            "xfce4-terminal",       // XFCE
            "mate-terminal",        // MATE
            "lxterminal",           // LXDE
            "xterm"                 // 最基本的终端
        };
        
        for (String terminal : terminals) {
            if (isCommandAvailable(terminal)) {
                return terminal;
            }
        }
        
        return null;
    }
    
    /**
     * 检查命令是否可用
     */
    private boolean isCommandAvailable(String command) {
        try {
            Process process = new ProcessBuilder("which", command).start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取进程 PID（尽力而为）
     */
    private String getPid(Process process) {
        try {
            // Java 9+ 有 pid() 方法，但我们需要兼容 Java 8
            // 使用反射尝试获取
            if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field pidField = process.getClass().getDeclaredField("pid");
                pidField.setAccessible(true);
                return String.valueOf(pidField.get(process));
            }
        } catch (Exception e) {
            // 忽略
        }
        return "unknown";
    }
    
    /**
     * 获取当前JAR路径
     */
    private String getCurrentJarPath() {
        try {
            String path = AppUpdater.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            
            // Windows路径处理
            if (isWindows() && path.startsWith("/")) {
                path = path.substring(1);
            }
            
            // 检查是否是目录（IDE 环境下运行）
            java.io.File file = new java.io.File(path);
            if (file.isDirectory()) {
                System.err.println("============================================");
                System.err.println("警告: 当前从 classes 目录运行，不是 JAR 文件！");
                System.err.println("路径: " + path);
                System.err.println("更新功能仅支持从 JAR 文件运行");
                System.err.println("============================================");
                
                // 在开发环境下，尝试找到打包好的JAR
                String projectRoot = file.getParentFile().getParentFile().getParentFile().getAbsolutePath();
                
                // 尝试1：查找 outJar 目录（发布目录）
                java.io.File outJarDir = new java.io.File(projectRoot, "outJar");
                if (outJarDir.exists() && outJarDir.isDirectory()) {
                        java.io.File[] jars = outJarDir.listFiles((dir, name) -> 
                        name.startsWith("PotatoTool") && name.endsWith(".jar"));
                    if (jars != null && jars.length > 0) {
                        // 优先使用最新的JAR（按名称排序）
                        java.util.Arrays.sort(jars, (a, b) -> b.getName().compareTo(a.getName()));
                        String jarPath = jars[0].getAbsolutePath();
                        if (debugMode) System.out.println("✓ 找到打包的JAR: " + jarPath);
                        return jarPath;
                    }
                }
                
                // 尝试2：查找 target 目录
                java.io.File targetDir = new java.io.File(projectRoot, "target");
                if (targetDir.exists() && targetDir.isDirectory()) {
                    java.io.File[] jars = targetDir.listFiles((dir, name) -> 
                        name.startsWith("PotatoTool") && name.endsWith(".jar") && !name.contains("original"));
                    if (jars != null && jars.length > 0) {
                        java.util.Arrays.sort(jars, (a, b) -> b.getName().compareTo(a.getName()));
                        String jarPath = jars[0].getAbsolutePath();
                        if (debugMode) System.out.println("✓ 找到 target 目录的JAR: " + jarPath);
                        return jarPath;
                    }
                }
                
                System.err.println("✗ 未找到打包的JAR文件");
                System.err.println("建议操作：");
                System.err.println("  1. 运行: mvn package");
                System.err.println("  2. 从打包的JAR启动程序");
                System.err.println("  3. 或者手动指定JAR路径");
                System.err.println("============================================");
                
                // 返回一个明显的占位符，让用户知道需要修正
                // 不抛异常，允许脚本生成继续（用户可能在测试）
                return new java.io.File(projectRoot, "PotatoTool.jar").getAbsolutePath();
            }
            
            return path;
        } catch (Exception e) {
            System.err.println("获取JAR路径失败: " + e.getMessage());
            e.printStackTrace();
            // 降级方案：假设在当前目录
            return "PotatoTool.jar";
        }
    }
    
    /**
     * 获取当前运行的 Java 可执行文件路径
     * 这样可以确保更新后使用相同的 Java 版本启动
     */
    private String getJavaExecutablePath() {
        try {
            // 获取 Java 安装目录
            String javaHome = System.getProperty("java.home");
            
            if (javaHome == null || javaHome.isEmpty()) {
                // 如果获取不到，使用 PATH 中的 java
                return isWindows() ? "java.exe" : "java";
            }
            
            // 构建 java 可执行文件的完整路径
            String javaExecutable;
            if (isWindows()) {
                // Windows: java.home\bin\java.exe 或 java.home\bin\javaw.exe
                // 使用 javaw.exe (无控制台窗口) 如果可用
                Path javawPath = new java.io.File(javaHome, "bin\\javaw.exe").toPath();
                Path javaPath = new java.io.File(javaHome, "bin\\java.exe").toPath();
                
                if (Files.exists(javawPath)) {
                    javaExecutable = javawPath.toString();
                } else if (Files.exists(javaPath)) {
                    javaExecutable = javaPath.toString();
                } else {
                    javaExecutable = "java.exe";
                }
            } else {
                // Unix/Linux/macOS: java.home/bin/java
                Path javaPath = new java.io.File(javaHome, "bin/java").toPath();
                
                if (Files.exists(javaPath)) {
                    javaExecutable = javaPath.toString();
                } else {
                    javaExecutable = "java";
                }
            }
            
            if (debugMode) System.out.println("检测到 Java 路径: " + javaExecutable);
            return javaExecutable;
            
        } catch (Exception e) {
            System.err.println("获取 Java 路径失败: " + e.getMessage());
            // 降级方案：使用系统默认的 java
            return isWindows() ? "java.exe" : "java";
        }
    }
    
    /**
     * 从URL提取JAR文件名
     */
    private String extractJarName(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0) {
            return url.substring(lastSlash + 1);
        }
        return "PotatoTool-new.jar";
    }
    
    /**
     * 判断是否是Windows系统
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}

