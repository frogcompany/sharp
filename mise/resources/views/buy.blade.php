<html lang="ja">

<head>
    <meta charset='utf-8'/>
    <title>MiseAI:商品購入確認</title>
</head>

<body>
{{-- フォーム --}}
<form method="POST" action="/complete">
<h1>最終確認</h1>

    <h2>サッポロ一番味噌ラーメン</h2>
    <h5 align="right">食品</h5>
    <h5>税込価格: 216円(税抜:200円 税額:16円)</h5>
    @csrf

    <p>支払い方法:クレジットカード[末尾:0123]</p>
    <p>配送先:東京都港区なんとか 徳川家康</p>
    <input type="hidden" name="id" value="1">
    <button type="submit">購入</button>
</form>
</body>

</html>

