<?php
header('Content-Type: application/json');

// Récupère les paramètres de la requête GET
$params = $_GET;

// Prépare une réponse JSON
$response = [
    'status' => 'success',
    'message' => 'GET request received',
    'params' => $params,
    'data' => [
        'item1' => 'This is item 1',
        'item2' => 'This is item 2',
        'item3' => 'This is item 3'
    ]
];

// Retourne la réponse JSON
echo json_encode($response);
?>
