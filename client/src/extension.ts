import * as path from "path";
import * as net from 'net';
import fs = require('fs');
import child_process = require('child_process');
const decompress = require("decompress");
import { workspace, ExtensionContext, window, commands, Uri, MessageItem, StatusBarAlignment, ProgressLocation, Progress } from "vscode";

import {
  LanguageClient,
  LanguageClientOptions,
  StreamInfo,
} from "vscode-languageclient/node";

import * as selectVersion from "./selectVersion";
import * as updateVersions from "./updateVersions";

let client: LanguageClient;
let serverProcess: child_process.ChildProcess;
let netServer: net.Server;

export function activate(context: ExtensionContext) {
  const outputChannel = window.createOutputChannel("Datapack Language Server");
  
  const statusBarItem = window.createStatusBarItem(StatusBarAlignment.Right, 100);
  statusBarItem.command = 'datapack-language-server.select-version'
  context.subscriptions.push(statusBarItem);

  function updateStatusBar() {
    statusBarItem.text = `Datapack Language Server: ${context.workspaceState.get<string>('selectedVersion') || 'No version selected'}`;
    statusBarItem.show();
  }

  async function updateLanguageServer() {
    updateStatusBar();

    const selectedVersion = context.workspaceState.get<string>('selectedVersion');
    const selectedFullVersion = context.workspaceState.get<string>('selectedFullVersion');
  
    if (client) {
      if (client.isRunning()) {
        await client.stop();
      }
      client = null;
    }

    if (netServer) {
      if (netServer.listening) {
        netServer.close();
      }
      netServer = null;
    }

    if (serverProcess) {
      serverProcess.kill(2);
      serverProcess = null;
    }

    if (selectedVersion && selectedFullVersion) {
      // TODO get the jar file path from the user
      const pluginDir = Uri.joinPath(context.globalStorageUri, 'versions', selectedVersion, selectedFullVersion, 'plugins')
      const pluginJar = Uri.joinPath(pluginDir, 'plugin.jar')
      if (!fs.existsSync(pluginJar.fsPath)) {
        if (!fs.existsSync(pluginDir.fsPath)) {
          fs.mkdirSync(pluginDir.fsPath);
        }

        await window.withProgress({
            location: ProgressLocation.Notification,
            title: "Downloading Plugin",
            cancellable: true
        }, async (progress, token) => {
            token.onCancellationRequested(() => {
                console.log("User canceled the long running operation");
            });

            progress.report({ increment: 0 });

            await downloadPlugin(selectedVersion, pluginDir, pluginJar, progress);
        });
      }


      // Options to control the language client
      const clientOptions: LanguageClientOptions = {
        // Register the server for all documents by default
        documentSelector: [{ scheme: "file", language: "mcfunction" }],
        outputChannel,
        //synchronize: {
          // Notify the server about file changes to '.clientrc files contained in the workspace
          //fileEvents: workspace.createFileSystemWatcher("**/.clientrc"),
        //},
      };
    
      function createServer(): Promise<StreamInfo> {
        const versionDir = Uri.joinPath(context.globalStorageUri, 'versions', selectedVersion, selectedFullVersion)
        const versionJar = Uri.joinPath(versionDir, `${selectedFullVersion}.jar`)
        /*
        if (! fs.existsSync(path.resolve(versionDir.fsPath, 'plugins', 'server-1.0-SNAPSHOT-all.jar'))) {
          if (! fs.existsSync(path.resolve(versionDir.fsPath, 'plugins'))) {
            fs.mkdirSync(path.resolve(versionDir.fsPath, 'plugins'));
          }
          fs.copyFileSync(context.asAbsolutePath(path.join('server', 'build', 'libs', 'server-1.0-SNAPSHOT-all.jar')), path.resolve(versionDir.fsPath, 'plugins', 'server-1.0-SNAPSHOT-all.jar'));
        }
        */
    
        return new Promise((resolve, reject) => {  
          netServer = net.createServer((socket) => {
            resolve({
              reader: socket,
              writer: socket
            });
          });
    
          netServer.listen(8123, () => {
            if (! serverProcess) {
              let args = [
                '-Xms500M',
                '-Xmx500M',
                '-Dcom.mojang.eula.agree=true',
                '-jar',
                versionJar.fsPath,
                '--nogui',
              ]
      
              const javaExecutablePath = findJavaExecutable('java');
      
              const serverProcess = child_process.spawn(javaExecutablePath, args, {
                cwd: versionDir.fsPath
              });
      
              serverProcess.stdout.on('data', (data) => {
                outputChannel.append(data.toString());
              });
            }
          })
        });
      }

      // Create the language client and start the client.
      client = new LanguageClient(
        "datapack-language-server language-server-id",
        "datapack-language-server language server name",
        createServer,
        clientOptions
      );
    
      // Start the client. This will also launch the server
      window.withProgress({
        location: ProgressLocation.Notification,
        cancellable: false,
        title: "Starting Server",
      }, async (progress) => {
        await client.start();
      });
    } else {
      const selectVersionItem = {
        title: "Select version",
        command: "datapack-language-server.select-version"
      }
  
      window.showInformationMessage("No version selected", selectVersionItem).then((item) => commands.executeCommand(item.command));
    }
  }

  updateLanguageServer();

  context.subscriptions.push(commands.registerCommand('datapack-language-server.select-version', selectVersion.executeCommand(context, updateLanguageServer)));
  context.subscriptions.push(commands.registerCommand('datapack-language-server.update-available-versions', updateVersions.executeCommand(context)));
}

export function deactivate(): Thenable<void> | undefined {
  if (!client) {
    return undefined;
  }

  serverProcess.kill();
  return client.stop();
}

async function downloadPlugin(version: string, pluginDir: Uri, pluginJar: Uri, progress: Progress<{ message?: string; increment?: number }>) {
  const response = await fetch('https://api.github.com/repos/Chrotos/datapack-language-server/releases/latest');
  if (!response.ok) {
      window.showErrorMessage("Failed to download plugin");
      return;
  }

  const json: any = await response.json();
  let assets: any[] = json.assets;

  assets = assets.filter(artifact => artifact.name === `${version}.jar`);
  assets = assets.sort((a, b) => a.created_at < b.created_at ? 1 : -1);

  if (assets.length === 0) {
      window.showErrorMessage("No plugin builds for this version found");
      return;
  }

  const artifact = assets[assets.length - 1];

  const artifactUrl = artifact.browser_download_url
  const artifactResponse = await fetch(artifactUrl);
  if (!response.ok) {
      window.showErrorMessage("Failed to download plugin");
      return;
  }

  const contentLength = +artifactResponse.headers.get('Content-Length');
  const reader = artifactResponse.body.getReader();
  const chunks = [];

  let receivedLength = 0;
  let lastPercent = 0;

  while(true) {
      const {done, value} = await reader.read();

      if (done) {
          break;
      }

      chunks.push(value);

      receivedLength += value.length;
      const percent = Math.floor((receivedLength / contentLength) * 100);

      progress.report({ increment: percent - lastPercent });

      lastPercent = percent;
  }

  let chunksAll = new Uint8Array(receivedLength);
  let position = 0;
  for(let chunk of chunks) {
      chunksAll.set(chunk, position);
      position += chunk.length;
  }
  
  fs.writeFileSync(pluginJar.fsPath, chunksAll)

  window.showInformationMessage("Plugin Download complete");
}

// MIT Licensed code from: https://github.com/georgewfraser/vscode-javac
function findJavaExecutable(binname: string) {
	binname = correctBinname(binname);

	// First search each JAVA_HOME bin folder
	if (process.env['JAVA_HOME']) {
		let workspaces = process.env['JAVA_HOME'].split(path.delimiter);
		for (let i = 0; i < workspaces.length; i++) {
			let binpath = path.join(workspaces[i], 'bin', binname);
			if (fs.existsSync(binpath)) {
				return binpath;
			}
		}
	}

	// Then search PATH parts
	if (process.env['PATH']) {
		let pathparts = process.env['PATH'].split(path.delimiter);
		for (let i = 0; i < pathparts.length; i++) {
			let binpath = path.join(pathparts[i], binname);
			if (fs.existsSync(binpath)) {
				return binpath;
			}
		}
	}

	// Else return the binary name directly (this will likely always fail downstream) 
	return null;
}

function correctBinname(binname: string) {
	if (process.platform === 'win32')
		return binname + '.exe';
	else
		return binname;
}
