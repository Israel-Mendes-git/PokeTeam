#!/bin/sh
# Envia a chave de assinatura para os Secrets do repositório (uma vez só, depois de criar o repo).
# Uso (na raiz do projeto, com o gh logado):  sh tools/configurar_segredos.sh
# Lê storeFile e storePassword de keystore.properties (que fica fora do Git).
set -e
arquivo=$(grep '^storeFile=' keystore.properties | cut -d= -f2-)
senha=$(grep '^storePassword=' keystore.properties | cut -d= -f2-)
[ -f "$arquivo" ] || { echo "Chave não encontrada: $arquivo"; exit 1; }
base64 -w0 "$arquivo" | gh secret set POKETEAM_KEYSTORE_B64
printf '%s' "$senha" | gh secret set POKETEAM_KEYSTORE_SENHA
echo "Segredos POKETEAM_KEYSTORE_B64 e POKETEAM_KEYSTORE_SENHA configurados."
