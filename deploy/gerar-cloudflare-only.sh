#!/usr/bin/env bash
# Gera /etc/nginx/snippets/codeburguer-cloudflare-only.conf com os IPs atuais do Cloudflare
# (allow ...; deny all;). Rodar com sudo na instalação e quando o Cloudflare mudar a lista.
set -euo pipefail

destino=/etc/nginx/snippets/codeburguer-cloudflare-only.conf
temporario=$(mktemp)
trap 'rm -f "$temporario"' EXIT

{
    echo "# Gerado por deploy/gerar-cloudflare-only.sh em $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    for lista in ips-v4 ips-v6; do
        curl -fsS -m 20 "https://www.cloudflare.com/$lista" | while read -r faixa; do
            [ -n "$faixa" ] && echo "allow $faixa;"
        done
    done
    echo "deny all;"
} > "$temporario"

# Lista vazia ou truncada derrubaria o site inteiro: exige um mínimo de faixas.
if [ "$(grep -c '^allow ' "$temporario")" -lt 10 ]; then
    echo "Lista de IPs do Cloudflare incompleta; nada foi alterado." >&2
    exit 1
fi

anterior=""
if [ -f "$destino" ]; then
    anterior=$(mktemp)
    cp "$destino" "$anterior"
fi

install -m 0644 "$temporario" "$destino"
if ! nginx -t 2>/dev/null; then
    if [ -n "$anterior" ]; then cp "$anterior" "$destino"; else rm -f "$destino"; fi
    echo "nginx -t falhou; snippet anterior restaurado." >&2
    exit 1
fi
systemctl reload nginx
echo "Snippet atualizado: $(grep -c '^allow ' "$destino") faixas do Cloudflare."
