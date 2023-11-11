<html lang="ja">

<head>
    <meta charset='utf-8'/>
    <title>MiseAI:商品詳細</title>
</head>

<body>
{{-- フォーム --}}
<form method="POST" action="/detail/buy/">

    <h2>サッポロ一番味噌ラーメン</h2>
    <h5 align="right">食品</h5>
    <img src="https://miseai.site/img/koikedammy.jpg" width="100%" alt="商品画像"/>
    <h5>税込価格: 216円(税抜:200円 税額:16円)</h5>
    <p>アニメで小池さんが食べてた食品</p>
    <p>サンヨー食品、一袋 (軽減税率対象)</p>
    @csrf

    <p>支払い方法<br/>

        {{ Form::select(
        'pay',
        [1=>'クレジットカード[末尾:0123]',2=>'クレジットカード[末尾:9876]', 3=>'PayPay',99=>'支払方法追加']
    ) }}

    </p>
    <p>配送先<br/>
        {{ Form::select(
    'addr',
    [1=>'東京都港区なんとか 徳川家康',2=>'新潟県新潟市なんとか 上杉謙信',99=>'配送先追加']
    ) }}

    </p>

    <input type="hidden" name="id" value="1">

    <button type="submit">購入確認</button>
</form>
</body>

</html>

