# maven-plugin for Redpen

## 目的

mavenからredpenを実行するためのpluginです。
文書をasciidocなどで記述し、それを[asciidoctorのmaven-plugin](https://asciidoctor.org/docs/asciidoctor-maven-plugin/)
でhtml/pdf化することができます。
そのときに、ただhtml/pdf化するだけではなく、
文書構成をチェックしつつ、チェック結果がOKの場合のみhtml/pdf化しようとしたのが、このpluginの目的です。

## Redpenについて

Redpen本体がCentral Repositoryから取得可能なのが `1.9` までであったため、本pluginは `1.9` 対応となっています。

* [Redpen 1.9マニュアル](http://redpen.cc/docs/1.9/index_ja.html)
* Redpenは `1.10` から `JapaneseExpressionVariation` (日本語表記ゆれ) が使用可能になっている。

## 使い方

pom.xmlのpluginsに以下を追加します。

```xml
<!-- redpen-maven-pluginの設定例 -->
<plugin>
    <groupId>com.nobutnk</groupId>
    <artifactId>redpen-maven-plugin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <configuration>
        <inputFormat>asciidoc</inputFormat>
        <configFileName>redpen-conf-ja.xml</configFileName>
        <language>ja</language>
        <limit>2</limit>
        <resultFormat>json</resultFormat>
        <inputFile>src/main/docs</inputFile>
    </configuration>
    <executions>
        <execution>
            <id>redpen</id>
            <phase>test</phase>
            <goals>
                <goal>redpen</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

上記構成では、test phaseにてredpenでのチェックを実施します。

|Parameter |Required |説明 |Default |コマンドライン|
|---|---|---|---|---|
|inputFormat |- |チェックしたいドキュメントの種類。markdown(.md), asciidoc(.adoc)など。この値により、チェック対象ファイルの拡張子を決定します。 |plain |`-Dredpen.input.format=xxx` | |
|configFileName |● |redpenの動作を決定するconfファイルへのパス |- |`-Dredpen.config.name=xxx` |
|language |- |チェック対象文書の言語種別。en,ja |en |`-Dredpen.config.language=xxx` |
|limit |- |Redpenがエラーと判定するためのエラー検出数上限。この数未満ならSUCCESS | 1 |`-Dredpen.config.limit=xxx` |
|resultFormat |- |結果ファイルの出力フォーマット。targetに出力される。plain/plain2/jsonなど |plain |`-Dredpen.config.resultFormat=xxx` |
|inputFile |● |チェック対象ドキュメントを検索する起点のディレクトリパス |- |`-Dredpen.config.inputFile=xxx` |
|resultFileName |- |チェック結果ファイル名 |redpen-result.txt |`-Dredpen.config.resultFile=xxx` |

## ビルドについて

### 単体テスト

修正後、以下のコマンドにてredpenが動作し、結果ファイルが出力されることを確認します

```bash
mvn clean test
```

### 結合テスト

修正後、以下のコマンドにて、redpen-maven-pluginが作成され、pom.xmlへの記述を通して実行されることを確認します
確認内容は、redpenが動作し、結果ファイルが出力されることまで。

```bash
mvn clean install -P run-its
```
