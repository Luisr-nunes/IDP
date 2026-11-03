"""Pre-processa a imagem de um documento com OpenCV antes do OCR.

Uso: python scripts/preprocessar_documento.py arquivoEntrada arquivoSaida
"""
import sys

import cv2


def main() -> int:
    if len(sys.argv) != 3:
        print("uso: preprocessar_documento.py arquivoEntrada arquivoSaida", file=sys.stderr)
        return 2

    entrada, saida = sys.argv[1], sys.argv[2]
    imagem = cv2.imread(entrada)
    if imagem is None:
        print("nao foi possivel ler a imagem de entrada", file=sys.stderr)
        return 1

    cinza = cv2.cvtColor(imagem, cv2.COLOR_BGR2GRAY)
    sem_ruido = cv2.fastNlMeansDenoising(cinza, h=10)
    binaria = cv2.adaptiveThreshold(
        sem_ruido, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 31, 10
    )

    if not cv2.imwrite(saida, binaria):
        print("nao foi possivel gravar a imagem de saida", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
