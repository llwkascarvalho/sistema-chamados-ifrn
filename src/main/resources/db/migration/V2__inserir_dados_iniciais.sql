INSERT INTO setores (nome, sigla) VALUES
    ('Coordenacao de Tecnologia da Informacao e Comunicacao', 'COTIC'),
    ('Manutencao', 'MANUT');

INSERT INTO categorias (nome, descricao, setor_id) VALUES
    ('Rede e internet', 'Problemas de conexao com a rede e o Wi-Fi.',
        (SELECT id FROM setores WHERE sigla = 'COTIC')),
    ('Computadores e projetores', 'Problemas nos computadores e projetores do campus.',
        (SELECT id FROM setores WHERE sigla = 'COTIC')),
    ('Eletrica', 'Problemas com iluminacao, tomadas e instalacoes eletricas.',
        (SELECT id FROM setores WHERE sigla = 'MANUT')),
    ('Hidraulica', 'Problemas com torneiras, tubulacoes e vazamentos.',
        (SELECT id FROM setores WHERE sigla = 'MANUT'));

INSERT INTO usuarios (nome, email, matricula, tipo, setor_id) VALUES
    ('Ana Silva', 'ana.silva@example.com', '20260001', 'SOLICITANTE', NULL),
    ('Bruno Souza', 'bruno.souza@example.com', '20260002', 'SOLICITANTE', NULL),
    ('Carla Lima', 'carla.lima@example.com', '20260003', 'TECNICO',
        (SELECT id FROM setores WHERE sigla = 'COTIC')),
    ('Diego Santos', 'diego.santos@example.com', '20260004', 'TECNICO',
        (SELECT id FROM setores WHERE sigla = 'MANUT'));
