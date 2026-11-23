#!/usr/bin/env bash
# Troca a senha do funcionário "admin" (o banco.sql cria admin/admin) por uma senha aleatória.
# A senha nova fica só em ~/codeburguer-admin.txt (modo 0600) na VPS; nada é impresso.
set -euo pipefail

arquivo="$HOME/codeburguer-admin.txt"
senha=$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | cut -c1-20)
if [ "${#senha}" -lt 16 ]; then
    echo "Falha ao gerar a senha." >&2
    exit 1
fi
hash=$(printf '%s' "$senha" | md5sum | cut -d' ' -f1)

# -q tira a linha "UPDATE 1"; sobra só um id por funcionário alterado
alteradas=$(docker exec -i codeburguer-db psql -U postgres -d lanchonete -qtA -v ON_ERROR_STOP=1 \
    -c "UPDATE tb_funcionarios SET senha = '$hash' WHERE usuario = 'admin' RETURNING id_funcionario" | grep -c . || true)
if [ "$alteradas" != "1" ]; then
    echo "Esperava alterar 1 funcionário admin, alterou $alteradas." >&2
    exit 1
fi

umask 077
printf 'usuario=admin\nsenha=%s\n' "$senha" > "$arquivo"
chmod 600 "$arquivo"
echo "Senha do admin trocada. Guardada em $arquivo (0600)."
