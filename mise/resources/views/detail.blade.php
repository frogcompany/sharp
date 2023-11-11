<html lang="ja">

<head>
    <meta charset='utf-8'/>
    <title>MiseAI:商品詳細</title>
</head>

<body>
{{-- フォーム --}}
<form method="POST">
    <h2>サッポロ一番味噌ラーメン</h2>
    <h5 align="right">食品</h5>
    <img src="https://miseai.site/img/koikedammy.jpg" width="100%" alt="商品画像"/>
    <h5>税込価格: 216円(税抜:200円 税額:16円)</h5>
    <p>アニメで小池さんが食べてた食品</p>
    <p>サンヨー食品、一袋 (軽減税率対象)</p>
    @csrf
    <button type="submit">購入確認</button>
</form>
</body>

</html>

