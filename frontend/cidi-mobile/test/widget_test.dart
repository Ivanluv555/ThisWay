import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:cidi_mobile/main.dart';
import 'package:cidi_mobile/data/cidi_repository.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  testWidgets('home and profile render without overflow', (tester) async {
    SharedPreferences.setMockInitialValues({});
    final repo = CidiRepository();
    await repo.initialize();
    await tester.pumpWidget(CidiApp(repository: repo));
    await tester.pumpAndSettle();
    expect(find.text('去到不拥挤的地方'), findsOneWidget);
    expect(find.text('本地体验 · 示例内容与预测，非实时数据'), findsOneWidget);
    await tester.tap(find.text('我的'));
    await tester.pumpAndSettle();
    expect(find.text('我的收藏'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.tap(find.text('登录'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '测试旅行者');
    await tester.tap(find.text('进入本地体验'));
    await tester.pumpAndSettle();
    expect(find.text('测试旅行者'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
  testWidgets('narrow screen navigation and search remain usable', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    SharedPreferences.setMockInitialValues({});
    final repo = CidiRepository();
    await repo.initialize();
    await tester.pumpWidget(CidiApp(repository: repo));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    await tester.tap(find.text('动态'));
    await tester.pumpAndSettle();
    expect(find.text('此刻，有人在路上'), findsOneWidget);
    await tester.tap(find.text('分流'));
    await tester.pumpAndSettle();
    expect(find.text('避开人潮，拥抱自由'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.tap(find.byTooltip('搜索'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), '洪崖洞');
    await tester.testTextInput.receiveAction(TextInputAction.search);
    await tester.pumpAndSettle();
    expect(find.text('洪崖洞'), findsWidgets);
    expect(find.textContaining('请先登录演示账号'), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
